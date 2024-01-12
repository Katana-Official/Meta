package com.wallet.crypto.trustapp.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.wallet.crypto.trustapp.App
import com.wallet.crypto.trustapp.entity.ServiceException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONException
import org.web3j.crypto.*
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.utils.Numeric
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.security.SecureRandom
import java.security.Security


object FileUtils {
    // 钱包文件外置存储目录
    val WALLET_DIR: String = generaFilePath(getExternalDir(), "wallet")

    fun checkPermission(context: Context, permission: String): Boolean {

        val localPackageManager = context.applicationContext.packageManager
        return localPackageManager.checkPermission(permission, context.applicationContext.packageName) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasExternalMediaMounted(context: Context): Boolean {
        return ((Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable())
                && checkPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE"))
    }


    private fun generaFilePath(path: String, childFir: String): String {
        val generaFile = File(path, childFir)
        if (!generaFile.exists()) {
            generaFile.mkdir()
        }
        return generaFile.path
    }

    fun getDiskCacheDir(context: Context = App.context): String {
        var cacheDirectory: File
        if (hasExternalMediaMounted(context) && context.externalCacheDir != null) {
            cacheDirectory = context.externalCacheDir!!
        } else {
            cacheDirectory = context.cacheDir
        }
        if (cacheDirectory == null) {
            cacheDirectory = context.cacheDir
        }
        return cacheDirectory.path
    }

    /**
     * 这种目录下的文件在应用被卸载时不会被删除
     * 钱包等数据可以存放到这里
     *
     * @return
     */
    fun getExternalDir(context: Context = App.context): String {
        return if (hasExternalMediaMounted(context)) {
            context.filesDir.path
//            Environment.getExternalStorageDirectory().path
        } else {
            getDiskCacheDir(context)
        }
    }

    /**
     * 删除该目录下的文件
     *
     * @param path
     */
    fun delFile(path: String) {
        if (path.isNotEmpty()) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
data class Account(var address: String,
                   var dateAdded: Long
//                   var privateKey: String,
//                   var publicKey: String,
//                   var mnemonic: List<String>,
//                   var mnemonicPath: String,
//                   var keystore: String
) : Serializable {


}
class WalletStorage private constructor() {

    private val walletStorageName = "sp_wallets"

    private var accountsDb: MutableList<Account> = mutableListOf()


    init {
        load()
    }

    @Synchronized
    private fun add(account: Account): Boolean {
        for (i in accountsDb.indices)
            if (accountsDb[i].address.equals(account.address, true)) return false
        accountsDb.add(account)
        save()
        return true
    }

    /**
     * 获取钱包list
     * @return MutableList<Account>?
     */
    @Synchronized
    fun getAccounts(): MutableList<Account> {
        return accountsDb
    }
//
//    @Throws(CipherException::class, IOException::class, InvalidAlgorithmParameterException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class)
//    fun generateWalletFileWithKey(
//            password: String, privatekey: String, useFullScrypt: Boolean): Account {
//
//        val ecKeyPair = ECKeyPair.create(Hex.decode(privatekey))
//        return generateWalletFile(password, ecKeyPair, useFullScrypt)
//    }


    fun hasAddress(address: String): Boolean {
        for (account in accountsDb) {
            if (account.address.equals(getAddress(address), true)) {
                return true
            }
        }
        return false
    }
    fun setupBouncyCastle() {
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            ?: // Web3j will set up the provider lazily when it's first used.
            return
        if (provider.javaClass == BouncyCastleProvider::class.java) {
            // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
    /**
     * 通过密钥生成钱包
     * @param password String
     * @param ecKeyPair ECKeyPair
     * @param useFullScrypt Boolean
     * @return Account
     * @throws CipherException
     * @throws IOException
     */
    @Throws(CipherException::class, IOException::class)
    fun generateWalletFile(password: String, ecKeyPair: ECKeyPair = Keys.createEcKeyPair(), useFullScrypt: Boolean = true): Account {

        val walletFile: WalletFile
        if (useFullScrypt) {
//            walletFile = Wallet.createStandard(password, ecKeyPair)
            walletFile = Wallet.create(password, ecKeyPair, N, P)
        } else {
            walletFile = Wallet.createLight(password, ecKeyPair)
        }

        val address = if(walletFile.address.startsWith("0x"))
            walletFile.address.substring(2)
        else walletFile.address
        Log.i("WalletStorage", "File.pathSeparator:" + File.separator)
        Log.i("WalletStorage", "wallet file path:" + FileUtils.WALLET_DIR)
        val destination = File(FileUtils.WALLET_DIR, address)

        objectMapper.writeValue(destination, walletFile)
//        WalletUtils.generateWalletFile(password, ecKeyPair, destination, useFullScrypt)

        val account = Account(address, System.currentTimeMillis())
        add(account)
        return account
    }

    /**
     * 通过keystore.json文件导入钱包
     *
     * @param keystore 原json文件
     * @param pwd      json文件密码
     * @param newPwd      json文件新密码
     * @return
     */
    fun importWalletByKeystore(keystore: String, pwd: String): Account? {
        var credentials: Credentials? = null
        try {
            var walletFile: WalletFile? = null
            walletFile = objectMapper.readValue(keystore, WalletFile::class.java)

            //            WalletFile walletFile = new Gson().fromJson(keystore, WalletFile.class);
            credentials = Credentials.create(Wallet.decrypt(pwd, walletFile!!))
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ETHWalletUtils", e.toString())
        } catch (e: CipherException) {
            Log.e("ETHWalletUtils", e.toString())
            //            ToastUtils.showToast(R.string.load_wallet_by_official_wallet_keystore_input_tip);
            e.printStackTrace()
        }
        return if (credentials != null) {
            generateWalletFile(pwd, ecKeyPair = credentials.ecKeyPair)
        } else null
    }

    /**
     * 通过明文私钥导入钱包
     *
     * @param privateKey
     * @param pwd
     * @return
     */
    fun importWalletByPrivateKey(privateKey: String, pwd: String): Account? {
        val ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey))
        return generateWalletFile(pwd, ecKeyPair)
    }


    /**
     * 导出明文私钥
     *
     * @param walletId 钱包Id
     * @param pwd      钱包密码
     * @return
     */
    fun exportPrivateKey(pwd: String, walletAddress: String): String? {
        val credentials = getWalletCredentials(pwd, walletAddress)
        val keypair: ECKeyPair
        var privateKey: String? = null
        try {
            keypair = credentials.ecKeyPair
            privateKey = Numeric.toHexStringNoPrefixZeroPadded(keypair.privateKey, Keys.PRIVATE_KEY_LENGTH_IN_HEX)
        } catch (e: CipherException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return privateKey
    }

    /**
     * 导出keystore文件
     *
     * @param walletId
     * @param pwd
     * @return
     */
    fun exportKeystore(address: String): String? {
        val walletFile: WalletFile
        try {
            walletFile = objectMapper.readValue<WalletFile>(File(FileUtils.WALLET_DIR, getAddress(address)), WalletFile::class.java)
            return objectMapper.writeValueAsString(walletFile)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CipherException) {
            e.printStackTrace()
            throw ServiceException(e.message)
        }

        return null
    }

    /**
     * 获取 Credentials
     * @param password String
     * @param address String
     * @return Credentials
     * @throws IOException
     * @throws JSONException
     * @throws CipherException
     */
    @Throws(IOException::class, JSONException::class, CipherException::class)
    fun getWalletCredentials(password: String, address: String): Credentials {
        return WalletUtils.loadCredentials(password, File(FileUtils.WALLET_DIR, getAddress(address)))
    }

    /**
     * 删除钱包
     * @param address String
     */
    fun deleteWallet(address: String) {
        Log.e("aaron", "delete address:" + address)
        var position = -1
        for (i in accountsDb.indices) {
            if (accountsDb[i].address.equals(getAddress(address), true)) {
                position = i
                break
            }
        }
        if (position >= 0) {
            File(FileUtils.WALLET_DIR, getAddress(address)).delete()
            accountsDb.removeAt(position)
        }
        save()
    }

    /**
     * 修改钱包密码
     * @param address
     * @param oldPassword
     * @param newPassword
     * @return
     */
    fun modifyPassword(address: String, oldPassword: String, newPassword: String): Account {
        val credentials = getWalletCredentials(oldPassword, address)
        val keypair = credentials.ecKeyPair
        return generateWalletFile(newPassword, keypair)
    }


    private fun getAddress(address: String): String {
        var walletShadowed = address
        if (walletShadowed.startsWith("0x"))
            walletShadowed = walletShadowed.substring(2, walletShadowed.length)
        return walletShadowed
    }
    /**
     * @author Aaron
     * @email aaron@magicwindow.cn
     * @date 14/12/25 7:49 PM
     * @description
     */
    class SPHelper private constructor(private val context: Context?) {
        private val SP_KEY_DEFAULT = "persistent_data"
        private val TAG = "SPHelper"

        private val sp: SharedPreferences?
            get() {
                if (context == null) {
                    return null
                }

                try {
                    return context.getSharedPreferences(SP_KEY_DEFAULT, preferenceMode)
                } catch (ignored: OutOfMemoryError) {

                }

                return null
            }


        init {
            preferenceMode = Context.MODE_MULTI_PROCESS

        }

        fun putBoolean(key: String, value: Boolean) {
            sp?.let {
                val editor = it.edit()
                editor.putBoolean(key, value)
                editor.apply()
            }
        }

        fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            var value = defaultValue
            sp?.let {
                value = it.getBoolean(key, defaultValue)

            }
            return value
        }

        fun getBoolean(key: String): Boolean {
            var value = false
            sp?.let {
                value = it.getBoolean(key, false)

            }
            return value
        }

        fun putInt(key: String, value: Int) {
            sp?.let {
                val editor = it.edit()
                editor.putInt(key, value)
                editor.apply()
            }
        }

        fun getInt(key: String, defaultValue: Int): Int {
            var value = defaultValue
            sp?.let {
                value = it.getInt(key, defaultValue)

            }
            return value
        }

        fun getInt(key: String): Int {
            var value = 0
            sp?.let {
                value = it.getInt(key, 0)
            }
            return value
        }

        /*fun put(key: String,value:Any?){
            if (sp == null) {
                return
            }
            val editor = sp!!.edit()
            with(editor){
                when (value){
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Set<*> -> when{
                        value.toTypedArray().isArrayOf<String>() ->
                            putSet(key, value as Set<String>)
                    }
                    else -> apply()

                }
                apply()
            }

        }*/


        fun put(key: String, value: String?) {
            sp?.let {
                val editor = it.edit()
                editor.putString(key, value)
                editor.apply()
            }
        }

        fun remove(key: String) {
            sp?.let {
                val editor = it.edit()
                editor.remove(key)
                editor.apply()
            }
        }

        operator fun get(key: String, defaultValue: String): String {
            var value: String = defaultValue
            sp?.let {
                value = it.getString(key, defaultValue).toString()
            }
            return value
        }

        operator fun get(key: String): String {
            var value = ""
            sp?.let {
                value = it.getString(key, "").toString()
            }
            return value
        }

        fun putString(key: String, value: String?) {
            sp?.let {
                val editor = it.edit()
                editor.putString(key, value)
                editor.apply()
            }
        }

        fun getSet(key: String): Set<String> {
            var value: Set<String> = HashSet()
            sp?.let {
                value = it.getStringSet(key, value) as Set<String>
            }
            return value
        }

        fun getSet(key: String, defaultValue: Set<String>): Set<String> {
            var value: Set<String> = defaultValue
            sp?.let {
                value = it.getStringSet(key, defaultValue) as Set<String>
            }
            return value
        }

        fun putSet(key: String, set: Set<String>) {
            sp?.let {
                val editor = it.edit()
                editor.putStringSet(key, set)
                editor.apply()
            }
        }

        fun addSet(key: String, setValue: String) {
            sp?.let { sp_ ->
                val editor = sp_.edit()
                val set = sp_.getStringSet(key, HashSet())
                set?.add(setValue)
                editor.putStringSet(key, set)
                editor.apply()
            }
        }

        fun getString(key: String, defaultValue: String): String {
            var value: String = defaultValue
            sp?.let {
                value = it.getString(key, defaultValue).toString()
            }
            return value
        }

        fun getString(key: String): String {
            var value: String = ""
            sp?.let {
                value = it.getString(key, "").toString()
            }
            return value
        }

        fun putLong(key: String, value: Long) {
            sp?.let {
                val editor = it.edit()
                editor.putLong(key, value)
                editor.apply()
            }
        }

        fun getLong(key: String): Long {
            var value = 0L
            sp?.let {
                value = it.getLong(key, 0L)

            }
            return value
        }

        fun getLong(key: String, defaultValue: Long): Long {
            var value = defaultValue
            sp?.let {
                value = it.getLong(key, defaultValue)
            }
            return value
        }

        companion object {

            @SuppressLint("StaticFieldLeak")
            @Volatile
            private var defaultInstance: SPHelper? = null
            private var preferenceMode = Context.MODE_PRIVATE

            val TRACKING_DEVICE_ID = "device_id"         //device_id

            fun create(context: Context = App.context): SPHelper {
                if (defaultInstance == null) {
                    synchronized(SPHelper::class.java) {
                        if (defaultInstance == null) {
                            defaultInstance = SPHelper(context.applicationContext)
                        }
                    }
                }
                return defaultInstance!!
            }
        }
    }
    data class StorableAccounts(var account: MutableList<Account>)
    @Synchronized
    fun save() {
        val gson = Gson()

        SPHelper.create().put(walletStorageName, gson.toJson(StorableAccounts(accountsDb)))

    }

    @Synchronized
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun load() {
        val wallet = SPHelper.create().get(walletStorageName)
        val gson = Gson()
        val storableWallets = gson.fromJson(wallet, StorableAccounts::class.java)
        if (storableWallets != null)
            accountsDb = storableWallets.account

    }

    companion object {
        private var _instance: WalletStorage? = null
        private val objectMapper = ObjectMapperFactory.getObjectMapper()
        internal object SecureRandomUtils {

            private val SECURE_RANDOM: SecureRandom

            // Taken from BitcoinJ implementation
            // https://github.com/bitcoinj/bitcoinj/blob/3cb1f6c6c589f84fe6e1fb56bf26d94cccc85429/core/src/main/java/org/bitcoinj/core/Utils.java#L573
            private var isAndroid = -1

            val isAndroidRuntime: Boolean
                get() {
                    if (isAndroid == -1) {
                        val runtime = System.getProperty("java.runtime.name")
                        isAndroid = if (runtime != null && runtime == "Android Runtime") 1 else 0
                    }
                    return isAndroid == 1
                }

            init {
                if (isAndroidRuntime) {
                    LinuxSecureRandom()
                }
                SECURE_RANDOM = SecureRandom()
            }

            fun secureRandom(): SecureRandom {
                return SECURE_RANDOM
            }
        }
        /**
         * 随机
         */
        private val secureRandom = SecureRandomUtils.secureRandom()
        /**
         * 通用的以太坊基于bip44协议的助记词路径 （imtoken jaxx Metamask myetherwallet）
         */
        var ETH_JAXX_TYPE = "m/44'/60'/0'/0/0"
        var ETH_LEDGER_TYPE = "m/44'/60'/0'/0"
        var ETH_CUSTOM_TYPE = "m/44'/60'/1'/0/0"

        /**
         * CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
         */
        private val N = 1 shl 9
        /**
         * Parallelization parameter. Must be a positive integer less than or equal to Integer.MAX_VALUE / (128 * r * 8).
         */
        private val P = 1

        fun getInstance(): WalletStorage {
            if (_instance == null) {
                _instance = WalletStorage()
            }
            return _instance!!
        }
    }

}