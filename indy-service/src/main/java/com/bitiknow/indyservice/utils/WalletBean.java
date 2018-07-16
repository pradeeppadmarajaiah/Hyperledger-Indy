package com.bitiknow.indyservice.utils;

import org.hyperledger.indy.sdk.wallet.Wallet;

public class WalletBean {
	private String walletName;
	private String walletCredentail;
	private Wallet wallet;

	/**
	 * @return the walletName
	 */
	public String getWalletName() {
		return walletName;
	}

	/**
	 * @param walletName the walletName to set
	 */
	public void setWalletName(String walletName) {
		this.walletName = walletName;
	}

	/**
	 * @return the walletCredentail
	 */
	public String getWalletCredentail() {
		return walletCredentail;
	}

	/**
	 * @param walletCredentail the walletCredentail to set
	 */
	public void setWalletCredentail(String walletCredentail) {
		this.walletCredentail = walletCredentail;
	}

	/**
	 * @return the wallet
	 */
	public Wallet getWallet() {
		return wallet;
	}

	/**
	 * @param wallet the wallet to set
	 */
	public void setWallet(Wallet wallet) {
		this.wallet = wallet;
	}

	public WalletBean(String walletName, String walletCredentail, Wallet wallet) {
		super();
		this.walletName = walletName;
		this.walletCredentail = walletCredentail;
		this.wallet = wallet;
	}

	public WalletBean() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WalletBean [walletName=" + walletName + ", walletCredentail=" + walletCredentail + ", wallet=" + wallet
				+ "]";
	}

}
