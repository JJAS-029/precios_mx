package mx.acomo.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lo que la pantalla necesita saber de la compra de Plus. */
data class EstadoCompra(
    /** Precio con formato de Google Play ("$49.00"); null si aún no se pudo consultar. */
    val precio: String? = null,
    /** El pago se hizo pero está pendiente (por ejemplo, en efectivo en una tienda). */
    val pendiente: Boolean = false,
    /** Mensaje para el usuario después de intentar comprar. */
    val mensaje: String? = null,
)

/**
 * ¿A cómo? Plus con Google Play Billing: un producto integrado de pago único con id [PRODUCTO].
 * No hay servidor: la compra se confirma (acknowledge) desde la app y se vuelve a consultar
 * cada vez que se abre, así se restaura en otro teléfono con la misma cuenta.
 */
class Compras(contexto: Context, private val onPlus: (Boolean) -> Unit) {

    private val _estado = MutableStateFlow(EstadoCompra())
    val estado: StateFlow<EstadoCompra> = _estado.asStateFlow()

    private var producto: ProductDetails? = null

    private val cliente: BillingClient = BillingClient.newBuilder(contexto.applicationContext)
        .setListener { resultado, compras -> alActualizarCompras(resultado, compras) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun conectar() {
        if (cliente.isReady) {
            consultar()
            return
        }
        cliente.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultado: BillingResult) {
                if (resultado.responseCode == BillingResponseCode.OK) consultar()
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() reintenta en la siguiente llamada.
            }
        })
    }

    fun cerrar() = cliente.endConnection()

    /** Consulta el precio y las compras ya hechas (restaura Plus). */
    fun consultar() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCTO)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        cliente.queryProductDetailsAsync(params) { resultado, detalles ->
            if (resultado.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            producto = detalles.productDetailsList.firstOrNull { it.productId == PRODUCTO }
            _estado.update { it.copy(precio = producto?.oneTimePurchaseOfferDetails?.formattedPrice) }
        }
        cliente.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build(),
        ) { resultado, compras ->
            if (resultado.responseCode == BillingResponseCode.OK) procesar(compras, consultaCompleta = true)
        }
    }

    fun comprar(actividad: Activity) {
        val p = producto
        if (p == null) {
            _estado.update { it.copy(mensaje = "Plus aún no está disponible. Revisa tu conexión e intenta más tarde.") }
            conectar()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(p).build()),
            )
            .build()
        val r = cliente.launchBillingFlow(actividad, params)
        if (r.responseCode != BillingResponseCode.OK) {
            _estado.update { it.copy(mensaje = "No se pudo abrir la compra (${r.responseCode}).") }
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }

    private fun alActualizarCompras(resultado: BillingResult, compras: List<Purchase>?) {
        when (resultado.responseCode) {
            BillingResponseCode.OK -> procesar(compras.orEmpty(), consultaCompleta = false)
            BillingResponseCode.ITEM_ALREADY_OWNED -> consultar()
            BillingResponseCode.USER_CANCELED -> Unit
            else -> _estado.update { it.copy(mensaje = "La compra no se completó. Intenta de nuevo.") }
        }
    }

    /**
     * [consultaCompleta]: la lista es TODO lo que tiene el usuario (queryPurchases), así que si
     * Plus no aparece es que no lo tiene (o se reembolsó). En una actualización parcial no se quita.
     */
    private fun procesar(compras: List<Purchase>, consultaCompleta: Boolean) {
        val plus = compras.filter { PRODUCTO in it.products }
        val pagada = plus.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pendiente = pagada == null && plus.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        _estado.update {
            it.copy(
                pendiente = pendiente,
                mensaje = when {
                    pendiente -> "Tu pago está pendiente. Plus se activará en cuanto se confirme."
                    pagada != null && !consultaCompleta -> "¡Listo! Ya tienes ¿A cómo? Plus. Gracias por apoyar la app."
                    else -> it.mensaje
                },
            )
        }
        if (pagada != null) {
            onPlus(true)
            if (!pagada.isAcknowledged) {
                // Si no se confirma en 3 días, Google reembolsa la compra.
                cliente.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(pagada.purchaseToken).build(),
                ) { }
            }
        } else if (consultaCompleta) {
            onPlus(false)
        }
    }

    companion object {
        /** Id del producto integrado en Play Console (Monetizar → Productos integrados). */
        const val PRODUCTO = "plus"
    }
}
