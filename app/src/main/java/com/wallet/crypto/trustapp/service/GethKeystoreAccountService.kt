package com.wallet.crypto.trustapp.service

import com.wallet.crypto.trustapp.entity.Wallet
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import java.math.BigDecimal
import java.math.BigInteger

class GethKeystoreAccountService(
) : AccountKeystoreService {
    private val keyStore: WalletStorage = WalletStorage.getInstance()
    override fun createAccount(password: String): Single<Wallet> {
        return Single.fromCallable {
            WalletStorage.getInstance().setupBouncyCastle()
            Wallet(keyStore.generateWalletFile(password).address.lowercase())
        }.subscribeOn(Schedulers.io())
    }

    override fun importKeystore(
        store: String,
        password: String,
        newPassword: String?
    ): Single<Wallet> {
        return importKeystore(
            store,
            password,
            newPassword
        )
    }

    fun importKeystoreInternal(store: String, password: String, newPassWd : String): Single<Wallet> {
        return Single.fromCallable {
            val account = keyStore.importWalletByKeystore(store, password)
            keyStore.modifyPassword(
                store,
                password,
                newPassWd
            )
            Wallet(account?.address?.lowercase())
        }.subscribeOn(Schedulers.io())
    }

    /*fun importPrivateKey(privateKey: String): Single<Wallet> {
        val newPassword = ""
        return Single.fromCallable {
            val key = BigInteger(privateKey, PRIVATE_KEY_RADIX)
            val keypair = ECKeyPair.create(key)
            val walletFile = create(newPassword, keypair, N, P)
            ObjectMapper().writeValueAsString(walletFile)
        }.compose { upstream -> importKeystore(upstream.blockingGet(), newPassword) }
    }*/

    override fun importPrivateKey(privateKey: String, pwd: String): Single<Wallet> {
        return Single.fromCallable {
            val account = keyStore
                .importWalletByPrivateKey(privateKey, pwd)
            Wallet(account!!.address.lowercase())
        }
            .subscribeOn(Schedulers.io())
    }

    override fun exportAccount(
        wallet: Wallet,
        password: String,
        newPassword: String
    ): Single<String> {
        return Single
            .fromCallable<String> {
                keyStore.modifyPassword(
                    wallet.address,
                    password,
                    newPassword
                )
                keyStore.exportKeystore(wallet.address)!!
            }
            .subscribeOn(Schedulers.io())
    }

    override fun deleteAccount(address: String, password: String?): Completable {
        return deleteAccount(address)
    }

    override fun signTransaction(
        signer: Wallet,
        signerPassword: String,
        toAddress: String,
        amount: BigInteger,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        nonce: Long,
        data: ByteArray,
        chainId: Long
    ): Single<ByteArray> {
        return signTransaction(
            signer, signerPassword, toAddress, amount, gasPrice, gasLimit, nonce, String(data), chainId
        )
    }

    fun exportAccount(wallet: Wallet): Single<String> {
        return Single
            .fromCallable<String> { keyStore.exportKeystore(wallet.address)!! }
            .subscribeOn(Schedulers.io())
    }

    fun exportPrivateKey(pwd:String,wallet: Wallet): Single<String> {
        return Single
            .fromCallable<String> { keyStore.exportPrivateKey(pwd,wallet.address)!! }
            .subscribeOn(Schedulers.io())
    }

    fun deleteAccount(address: String): Completable {
        return Completable.fromCallable { keyStore.deleteWallet(address) }
            .subscribeOn(Schedulers.io())
    }

    fun signTransaction(signer: Wallet, signerPassword: String, toAddress: String, amount: BigInteger, gasPrice: BigInteger, gasLimit: BigInteger, nonce: Long, data: String?, chainId: Long): Single<ByteArray> {
        return Single.fromCallable {
            val keys = keyStore.getWalletCredentials(signerPassword, signer.address)

            val tx = RawTransaction.createTransaction(
                BigInteger(nonce.toString()),
                gasPrice,
                gasLimit,
                toAddress,
//                    BigDecimal(amount).multiply(ExchangeCalculator.ONE_ETHER).toBigInteger(),
                BigDecimal(amount).toBigInteger(),
                data.toString()
            )
            TransactionEncoder.signMessage(tx, chainId.toByte(), keys)
        }.subscribeOn(Schedulers.io())
    }

    override fun hasAccount(address: String): Boolean {
        return keyStore.hasAddress(address)
    }

    override fun fetchAccounts(): Single<Array<Wallet?>> {
        return Single.fromCallable<Array<Wallet?>> {
            val accounts = keyStore.getAccounts()
            val len = accounts.size
            val result = arrayOfNulls<Wallet>(len)

            for (i in 0 until len) {
                val gethAccount = accounts.get(i)
                result[i] = Wallet(generateAddress(gethAccount.address.lowercase()))
            }
            result
        }
            .subscribeOn(Schedulers.io())
    }

    private fun generateAddress(address: String): String {
        return address
    }

    companion object {
        private val PRIVATE_KEY_RADIX = 16
        /**
         * CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
         */
        private val N = 1 shl 9
        /**
         * Parallelization parameter. Must be a positive integer less than or equal to Integer.MAX_VALUE / (128 * r * 8).
         */
        private val P = 1
    }

}