package com.bitiknow.indyservice;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateAndStoreCredentialDef;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateCredential;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateCredentialOffer;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateSchema;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateCredentialReq;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateMasterSecret;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateProof;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverGetCredentialsForProofReq;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverStoreCredential;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.verifierVerifyProof;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bitiknow.indyservice.utils.PoolUtils;
import com.bitiknow.indyservice.utils.WalletBean;

@RestController
public class IndyServiceController {

	@GetMapping("/indy-service")
	public String welcome() {
		return "Welcome to Indy Service";

	}

	public static void main(String[] args) throws Exception {
		String stewardWalletName = "pradeep";
		String stewardWalletCredentials = "{\"key\":\"my_wallet_key\"}";
		String stewardSeed = "000000000000000000000000Steward1";

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.PROTOCOL_VERSION).get();

		String poolName = PoolUtils.createPoolLedgerConfig();

		Pool pool = createAndOpenPoolLedger(poolName);
		Wallet stewardWallet = createAndOpenStewardWallet(poolName, stewardWalletName, stewardWalletCredentials);
		DidResults.CreateAndStoreMyDidResult stewardDidResult = createAndStoreStewardDid(stewardWallet, stewardSeed);
		DidResults.CreateAndStoreMyDidResult trustAnchorDidResult = createAndStoreTrustAnchorDidBySteward(
				stewardWallet);
		String ledgerResponseForAddingTrustAnchor = sendRequestToLedger(pool, stewardWallet, stewardDidResult,
				trustAnchorDidResult);
		System.out.println("Trust Anchor submitted to Ledger. Ledger Response : "+ledgerResponseForAddingTrustAnchor );

		// Create Schema
		AnoncredsResults.IssuerCreateSchemaResult createSchemaResult = createCredentialSchema(trustAnchorDidResult);
		// TODO : Send to ledger

		// Create Credential Definition
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult = createCredentialSchemaDefintion(
				createSchemaResult, stewardWallet, stewardDidResult.getDid());
		// TODO : Send to ledger

		// Create and Open Prover Wallet
		String proverWalletName = "proverWallet";
		String proverDid = "VsKV7grR1BUE29mG2Fm2kX";
		String proverWalletCredentials = "{\"key\":\"prover_wallet_key\"}";
		Wallet.createWallet(poolName, proverWalletName, "default", null, proverWalletCredentials).get();
		Wallet proverWallet = Wallet.openWallet(proverWalletName, null, proverWalletCredentials).get();

		// Issuer Creates Credential offer
		System.out.println("\nIssuer Creates Credential Offer\n");
		String credOffer = issuerCreateCredentialOffer(stewardWallet, createCredDefResult.getCredDefId()).get();

		System.out.println("\nProver create Master Secret\n");
		String masterSecretId = proverCreateMasterSecret(proverWallet, null).get();

		// Prover Creates Credential Request
		System.out.println("\nProver Creates Credential Request\n");
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult = proverCreateCredentialReq(
				proverWallet, proverDid, credOffer, createCredDefResult.getCredDefJson(), masterSecretId).get();
		String credReqJson = createCredReqResult.getCredentialRequestJson();
		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
		System.out.println("Credential Request Json : " + credReqJson);
		System.out.println("Credential Request Meta data Json : " + credReqMetadataJson);

		// Issuer create Credential
		System.out.println("\nIssuer create Credential\n");
		String credValuesJson = new JSONObject("{\n"
				+ "        \"companyName\": {\"raw\": \"RegencyManning\", \"encoded\": \"594465709955896723921094925839488742869205008160769251991705001\"},\n"
				+ "        \"employeeId\": {\"raw\": \"RM01\", \"encoded\": \"1139481716457488690172217916278103335\"},\n"
				+ "        \"employeeName\": {\"raw\": \"Pradeep P\", \"encoded\": \"175125\"},\n"
				+ "        \"designation\": {\"raw\": \"Blockchain Developer\", \"encoded\": \"1712\"},\n"
				+ "        \"startDate\": {\"raw\": \"11/11/2017\", \"encoded\": \"171\"},\n"
				+ "        \"endDate\": {\"raw\": \"N-A\", \"encoded\": \"17\"},\n"
				+ "        \"salary\": {\"raw\": \"222\", \"encoded\": \"28\"}\n" + "    }").toString();

		AnoncredsResults.IssuerCreateCredentialResult createCredentialResult = issuerCreateCredential(stewardWallet,
				credOffer, credReqJson, credValuesJson, null, -1).get();
		String credential = createCredentialResult.getCredentialJson();

		// Prover Stores Credential
		System.out.println("\nProver Stores Credential\n");
		proverStoreCredential(proverWallet, null, credReqMetadataJson, credential, createCredDefResult.getCredDefJson(),
				null).get();

		// Prover Gets Credentials for Proof Request
		System.out.println("\nProver Gets Credentials for Proof Request\n");
		String proofRequestJson = new JSONObject("{" + "                    \"nonce\":\"123432421212\",\n"
				+ "                    \"name\":\"loan_req_1\",\n" + "                    \"version\":\"0.1\", "
				+ "                    \"requested_attributes\": {"
				+ "                          \"attr1_referent\":{\"name\":\"employeeName\"},"
				+ "                          \"attr2_referent\":{\"name\":\"salary\"},"
				+ "                          \"attr3_referent\":{\"name\":\"phone\"}" + "                     },"
				+ "                    \"requested_predicates\":{"
				+ "                         \"predicate1_referent\":{\"name\":\"salary\",\"p_type\":\">=\",\"p_value\":18}"
				+ "                    }" + "                  }").toString();
		String credentialsForProofJson = proverGetCredentialsForProofReq(proverWallet, proofRequestJson).get();

		JSONObject credentialsForProof = new JSONObject(credentialsForProofJson);
		JSONArray credentialsForAttribute1 = credentialsForProof.getJSONObject("attrs").getJSONArray("attr1_referent");

		String credentialId = credentialsForAttribute1.getJSONObject(0).getJSONObject("cred_info")
				.getString("referent");

		// Prover Creates Proof
		System.out.println("\nProver Creates Proof\n");
		String selfAttestedValue = "8197922363";
		String requestedCredentialsJson = new JSONObject(String.format("{\n"
				+ "                                          \"self_attested_attributes\":{\"attr3_referent\":\"%s\"},\n"
				+ "                                          \"requested_attributes\":{\"attr1_referent\":{\"cred_id\":\"%s\", \"revealed\":true},\n"
				+ "                                                                    \"attr2_referent\":{\"cred_id\":\"%s\", \"revealed\":false}},\n"
				+ "                                          \"requested_predicates\":{\"predicate1_referent\":{\"cred_id\":\"%s\"}}\n"
				+ "                                        }", selfAttestedValue, credentialId, credentialId,
				credentialId)).toString();

		String schemas = new JSONObject(
				String.format("{\"%s\":%s}", createSchemaResult.getSchemaId(), createSchemaResult.getSchemaJson()))
						.toString();
		String credentialDefs = new JSONObject(
				String.format("{\"%s\":%s}", createCredDefResult.getCredDefId(), createCredDefResult.getCredDefJson()))
						.toString();
		String revocStates = new JSONObject("{}").toString();

		String proofJson = proverCreateProof(proverWallet, proofRequestJson, requestedCredentialsJson, masterSecretId,
				schemas, credentialDefs, revocStates).get();

		System.out.println("\nVerifier verify Proof\n");
		String revocRegDefs = new JSONObject("{}").toString();
		String revocRegs = new JSONObject("{}").toString();

		Boolean valid = verifierVerifyProof(proofRequestJson, proofJson, schemas, credentialDefs, revocRegDefs,
				revocRegs).get();
		System.out.println(valid);

		java.util.List<WalletBean> wallets = new ArrayList<>();
		wallets.add(new WalletBean(stewardWalletName, stewardWalletCredentials, stewardWallet));
		wallets.add(new WalletBean(proverWalletName, proverWalletCredentials, proverWallet));
		// Close and Delete Wallets
		closeAndDeleteWallet(wallets);

		Map<Pool, String> pools = new HashMap<>();
		pools.put(pool, poolName);
		// Close and delete pools
		closeAndDeletePool(pools);
	}

	/**
	 * 
	 * @param pools
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static void closeAndDeletePool(Map<Pool, String> pools)
			throws InterruptedException, ExecutionException, IndyException {

		for (Map.Entry<Pool, String> pool : pools.entrySet()) {
			System.out.println("\nClose pool\n");
			pool.getKey().closePoolLedger().get();

			System.out.println(" Delete pool ledger config\n");
			Pool.deletePoolLedgerConfig(pool.getValue()).get();
		}
	}

	/**
	 * 
	 * @param wallets
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static void closeAndDeleteWallet(java.util.List<WalletBean> wallets)
			throws InterruptedException, ExecutionException, IndyException {
		for (WalletBean wallet : wallets) {
			System.out.println("\nClose and deleting n" + wallet.getWalletName());
			wallet.getWallet().closeWallet().get();
			Wallet.deleteWallet(wallet.getWalletName(), wallet.getWalletCredentail()).get();
		}
	}

	/**
	 * 
	 * @param trustAnchorDidResult
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static AnoncredsResults.IssuerCreateSchemaResult createCredentialSchema(
			DidResults.CreateAndStoreMyDidResult trustAnchorDidResult)
			throws InterruptedException, ExecutionException, IndyException {
		System.out.println("Build the SCHEMA request\n");
		String schemaName = "experienceCertificate";
		String schemaVersion = "1.0";
		String schemaAttributes = "[\"companyName\", \"employeeId\",\"employeeName\", \"designation\", \"startDate\",\"endDate\",\"salary\"]";
		AnoncredsResults.IssuerCreateSchemaResult createSchemaResult = issuerCreateSchema(trustAnchorDidResult.getDid(),
				schemaName, schemaVersion, schemaAttributes).get();
		System.out.println("Credential Schema Id : " + createSchemaResult.getSchemaId());
		System.out.println("Credential Schema Json : " + createSchemaResult.getSchemaJson());
		// TODO : Send to ledger
		return createSchemaResult;
	}

	/**
	 * 
	 * @param createSchemaResult
	 * @param wallet
	 * @param did
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredentialSchemaDefintion(
			AnoncredsResults.IssuerCreateSchemaResult createSchemaResult, Wallet wallet, String did)
			throws InterruptedException, ExecutionException, IndyException {
		System.out.println("Build the SCHEMA DEFINITION request to use the schema \n");
		String credDefTag = "EXPERIENCE_CERT";
		String credDefConfigJson = "{\"support_revocation\":false}";
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult = issuerCreateAndStoreCredentialDef(
				wallet, did, createSchemaResult.getSchemaJson(), credDefTag, null, credDefConfigJson).get();
		System.out.println("Credential Definition Id : " + createCredDefResult.getCredDefId());
		System.out.println("Credential Defintion Json : " + createCredDefResult.getCredDefJson());
		return createCredDefResult;
	}

	/**
	 * 
	 * @return
	 */
	public static Pool createAndOpenPoolLedger(String poolName) {
		// String poolName ="default_pool";
		Pool pool = null;
		try {

			System.out.println(
					"\n1. Creating a new local pool ledger configuration that can be used later to connect pool nodes.\n");

			System.out.println("\n2. Open pool ledger and get the pool handle from libindy.\n");
			pool = Pool.openPoolLedger(poolName, "{}").get();
		} catch (InterruptedException | ExecutionException | IndyException e) {
			e.printStackTrace();
		}
		return pool;
	}

	/**
	 * 
	 * @param poolName
	 * @param walletName
	 * @param walletCredentials
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static Wallet createAndOpenStewardWallet(String poolName, String walletName, String walletCredentials)
			throws InterruptedException, ExecutionException, IndyException {

		System.out.println("\n3. Creates a new secure wallet\n");
		Wallet.createWallet(poolName, walletName, "default", null, walletCredentials).get();

		System.out.println("\n4. Open wallet and get the wallet handle from libindy\n");
		return Wallet.openWallet(walletName, null, walletCredentials).get();
	}

	/**
	 * 
	 * @param wallet
	 * @param stewardSeed
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static DidResults.CreateAndStoreMyDidResult createAndStoreStewardDid(Wallet wallet, String stewardSeed)
			throws InterruptedException, ExecutionException, IndyException {
		System.out.println("\n5. Generating and storing steward DID and Verkey\n");
		String did_json = "{\"seed\": \"" + stewardSeed + "\"}";
		DidResults.CreateAndStoreMyDidResult stewardResult = Did.createAndStoreMyDid(wallet, did_json).get();
		String defaultStewardDid = stewardResult.getDid();
		System.out.println("Steward DID: " + defaultStewardDid);
		System.out.println("Steward Verkey: " + stewardResult.getVerkey());

		return stewardResult;

	}

	/**
	 * 
	 * @param wallet
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static DidResults.CreateAndStoreMyDidResult createAndStoreTrustAnchorDidBySteward(Wallet wallet)
			throws InterruptedException, ExecutionException, IndyException {
		// 6.
		System.out.println("\n6. Generating and storing Trust Anchor DID and Verkey\n");
		DidResults.CreateAndStoreMyDidResult trustAnchorResult = Did.createAndStoreMyDid(wallet, "{}").get();
		String trustAnchorDID = trustAnchorResult.getDid();
		String trustAnchorVerkey = trustAnchorResult.getVerkey();
		System.out.println("Trust anchor DID: " + trustAnchorDID);
		System.out.println("Trust anchor Verkey: " + trustAnchorVerkey);
		return trustAnchorResult;
	}

	/***
	 * 
	 * @param pool
	 * @param wallet
	 * @param stewardDidResult
	 * @param trustAnchorDidResult
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IndyException
	 */
	public static String sendRequestToLedger(Pool pool, Wallet wallet,
			DidResults.CreateAndStoreMyDidResult stewardDidResult,
			DidResults.CreateAndStoreMyDidResult trustAnchorDidResult)
			throws InterruptedException, ExecutionException, IndyException {

		System.out.println("\n7. Build NYM request to add Trust Anchor to the ledger\n");
		String nymRequest = buildNymRequest(stewardDidResult.getDid(), trustAnchorDidResult.getDid(),
				trustAnchorDidResult.getVerkey(), null, "TRUST_ANCHOR").get();
		System.out.println("NYM request JSON:\n" + nymRequest);
		System.out.println("\n8. Sending the nym request to ledger\n");
		String nymResponseJson = signAndSubmitRequest(pool, wallet, stewardDidResult.getDid(), nymRequest).get();
		System.out.println("NYM transaction response:\n" + nymResponseJson);
		return nymResponseJson;
	}

}
