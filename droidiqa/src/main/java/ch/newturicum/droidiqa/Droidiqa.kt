package ch.newturicum.droidiqa

import android.content.Context
import ch.newturicum.droidiqa.repository.DroidiqaRepositoryImpl
import ch.newturicum.droidiqa.security.KeyEncoder
import com.android.volley.RequestQueue


/**
 * The Client repository with access to the Zilliqa blockchain. It is recommended to instantiate this
 * as a singleton.
 * @param applicationContext The application context.
 * @param keyEncoder Optional. A customized implementation used to encode / decode private keys locally.
 * The default implementation uses a symmetric key encryption via the local Android keystore.
 * @param volleyRequestQueue Optional. If you want to integrate the client with your already existing
 * request queue, you can pass it here. Default: The client maintains its own request queue. Droidiqa
 * will manage its request lifetimes internally.
 */
class Droidiqa(
    applicationContext: Context,
    keyEncoder: KeyEncoder? = null,
    volleyRequestQueue: RequestQueue? = null
) : DroidiqaRepositoryImpl(applicationContext, keyEncoder, volleyRequestQueue)