package com.bitiknow.indyservice;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bitiknow.indyservice.utils.PoolUtils;

@RestController
public class IndyServiceController {

	@GetMapping("/indy-service")
	public String welcome() {
		return "Welcome to Indy Service";
	}

	/**
	 * 
	 * @return
	 */
	public Pool createAndOpenPoolLedger() {
		// String poolName ="default_pool";
		Pool pool = null;
		String poolName;
		try {
			poolName = PoolUtils.createPoolLedgerConfig();
			pool = Pool.openPoolLedger(poolName, "{}").get();
		} catch (InterruptedException | ExecutionException | IndyException | IOException e) {
			e.printStackTrace();
		}
		return pool;
	}
	
	
}
