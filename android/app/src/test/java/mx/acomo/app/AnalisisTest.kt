package mx.acomo.app

import kotlinx.serialization.json.Json
import mx.acomo.app.data.DatosPrecios
import mx.acomo.app.data.PrecioSemana
import mx.acomo.app.data.Producto
import mx.acomo.app.data.Semaforo
import mx.acomo.app.data.coincide
import mx.acomo.app.data.etiquetaSemana
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.pesos
import mx.acomo.app.data.resumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnalisisTest {

    private val jitomate = Producto(
        id = "tomate-saladette-primera", nombre = "Tomate Saladette", calidad = "Primera",
        categoria = "Hortalizas",
        semanas = listOf(
            PrecioSemana("2026-W45", "150", 25.0, 30.0, 30.0, 5),
            PrecioSemana("2026-W46", "150", 25.0, 40.0, 33.0, 5),
            PrecioSemana("2026-W46", "151", 32.0, 41.0, 37.5, 5),
        ),
    )

    @Test fun busquedaPorNombreComunYSinAcentos() {
        assertTrue(jitomate.coincide("jitomate"))
        assertTrue(jitomate.coincide("JITOMATE saladette"))
        assertTrue(jitomate.coincide("tomate"))
        assertFalse(jitomate.coincide("aguacate"))
        val limon = jitomate.copy(nombre = "Limón s/semilla")
        assertTrue(limon.coincide("limon"))
    }

    @Test fun resumenPorMercado() {
        val r = jitomate.resumen("150")!!
        assertEquals(33.0, r.actual.frec, 0.001)
        assertEquals(10.0, r.cambioSemanal!!, 0.001)
        assertEquals(31.5, r.promedio8, 0.001)
        assertEquals(null, jitomate.resumen("151")!!.cambioSemanal)
        assertEquals(null, jitomate.resumen("999"))
    }

    @Test fun semaforo() {
        // Referencia = 30 × 1.5 = 45
        assertEquals(Semaforo.VERDE, evaluarPrecio(40.0, 30.0, "Hortalizas").semaforo)
        assertEquals(Semaforo.VERDE, evaluarPrecio(45.0, 30.0, "Hortalizas").semaforo)
        assertEquals(Semaforo.AMARILLO, evaluarPrecio(50.0, 30.0, "Hortalizas").semaforo)
        assertEquals(Semaforo.ROJO, evaluarPrecio(60.0, 30.0, "Hortalizas").semaforo)
    }

    @Test fun formatos() {
        assertEquals("9–13 nov", etiquetaSemana("2026-W46"))
        assertEquals("29 dic–2 ene", etiquetaSemana("2026-W01"))
        assertEquals("$37.50", pesos(37.5))
        assertEquals("$5.05", pesos(5.049))
    }

    /** El JSON real que genera el recolector debe poder leerse. */
    @Test fun leeJsonDelRecolector() {
        val archivo = File("../../data/precios.json")
        if (!archivo.exists()) return
        val datos = Json { ignoreUnknownKeys = true }.decodeFromString<DatosPrecios>(archivo.readText())
        assertTrue(datos.productos.size > 50)
        assertNotNull(datos.productos.find { it.id == "tomate-saladette-primera" }?.resumen("150"))
    }
}
