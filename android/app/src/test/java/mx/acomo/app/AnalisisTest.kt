package mx.acomo.app

import kotlinx.serialization.json.Json
import mx.acomo.app.data.DatosMercado
import mx.acomo.app.data.Indice
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.cercanos
import mx.acomo.app.data.distanciaA
import mx.acomo.app.data.PrecioSemana
import mx.acomo.app.data.Producto
import mx.acomo.app.data.Semaforo
import mx.acomo.app.data.coincide
import mx.acomo.app.data.etiquetaSemana
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.pesos
import mx.acomo.app.data.resumen
import mx.acomo.app.data.ArticuloMandado
import mx.acomo.app.data.cantidad
import mx.acomo.app.data.mandadoTexto
import mx.acomo.app.data.masKg
import mx.acomo.app.data.menosKg
import mx.acomo.app.data.presupuesto
import mx.acomo.app.data.alternativas
import mx.acomo.app.data.sugerenciasCambio
import mx.acomo.app.data.bajadas
import mx.acomo.app.data.textoAviso
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
            PrecioSemana("2026-W46", 25.0, 40.0, 33.0, 5),
            PrecioSemana("2026-W45", 25.0, 30.0, 30.0, 5),
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

    /** Producto con 8 semanas a [base] y la última a [actual]. */
    private fun prod(nombre: String, base: Double, actual: Double, categoria: String = "Hortalizas") = Producto(
        id = normalizarId(nombre), nombre = nombre, calidad = "Primera", categoria = categoria,
        semanas = (39..45).map { PrecioSemana("2026-W$it", null, null, base, 5) } +
            PrecioSemana("2026-W46", null, null, actual, 5),
    )
    private fun normalizarId(n: String) = n.lowercase().replace(' ', '-')

    @Test fun equivalencias() {
        val saladette = prod("Tomate Saladette", 30.0, 45.0)   // caro
        val verde = prod("Tomate Verde", 20.0, 14.0)           // barato
        val bola = prod("Tomate Bola", 30.0, 33.0)
        val papa = prod("Papa Alpha", 18.0, 18.0)
        val papaya = prod("Papaya Maradol", 20.0, 15.0, "Frutas")
        val camote = prod("Camote", 15.0, 12.0)
        val todos = listOf(saladette, verde, bola, papa, papaya, camote)

        val alts = alternativas(saladette, todos)
        assertEquals(listOf("Tomate Verde", "Tomate Bola"), alts.map { it.producto.nombre })
        assertEquals("para salsa", alts.first().motivo)

        // "papa " no debe confundirse con "papaya"
        assertEquals(listOf("Camote"), alternativas(papa, todos).map { it.producto.nombre })

        val cambios = sugerenciasCambio(todos)
        assertEquals(1, cambios.size)
        assertEquals("Tomate Saladette", cambios[0].caro.nombre)
        assertEquals("Tomate Verde", cambios[0].alternativa.producto.nombre)

        // Ciruela pasa no sustituye a la ciruela fresca; los chiles no se agrupan por "chile".
        val ciruela = prod("Ciruela Roja", 30.0, 30.0, "Frutas")
        val pasa = prod("Ciruela pasa", 80.0, 60.0, "Frutas")
        assertTrue(alternativas(ciruela, listOf(ciruela, pasa)).isEmpty())
        val serrano = prod("Chile Serrano", 30.0, 30.0)
        val guajillo = prod("Chile Guajillo", 90.0, 80.0, "Chiles secos")
        assertTrue(alternativas(serrano, listOf(serrano, guajillo)).isEmpty())

        // Tamaños del mismo producto no son alternativas entre sí.
        val limon4 = prod("Limón c/semilla # 4", 20.0, 25.0)
        val limon5 = prod("Limón c/semilla # 5", 18.0, 15.0)
        val sinSemilla = prod("Limón s/semilla", 22.0, 20.0)
        assertEquals(listOf("Limón s/semilla"), alternativas(limon4, listOf(limon4, limon5, sinSemilla)).map { it.producto.nombre })
    }

    @Test fun avisosDeBajada() {
        val jit = prod("Tomate Saladette", 30.0, 24.0)    // -20%
        val cebolla = prod("Cebolla Blanca", 20.0, 19.0)  // -5%: no avisa
        val papa = prod("Papa Alpha", 25.0, 20.0)         // -20%, pero no se sigue
        // Barato contra su promedio pero sin bajar esta semana: ya se avisó antes, no repetir.
        val limon = Producto("limon", "Limon", "Primera", "Frutas",
            (39..44).map { PrecioSemana("2026-W$it", null, null, 30.0, 5) } +
                listOf(PrecioSemana("2026-W45", null, null, 20.0, 5), PrecioSemana("2026-W46", null, null, 20.0, 5)))
        val todos = listOf(jit, cebolla, papa, limon)
        val seguidos = setOf(jit.id, cebolla.id, limon.id)

        val b = bajadas(seguidos, todos, "2026-W46")
        assertEquals(listOf(jit.id), b.map { it.producto.id })
        assertEquals("-20% vs. la semana pasada", b[0].motivo)
        // Una semana que no es la más reciente del producto: no avisar de precios viejos.
        assertTrue(bajadas(seguidos, todos, "2026-W47").isEmpty())

        val (titulo, lineas) = textoAviso(b, "Toluca")
        assertEquals("Bajó jitomate saladette en Toluca", titulo)
        assertEquals("Jitomate saladette: $24.00/kg (-20% vs. la semana pasada)", lineas.single())
        assertEquals("Bajaron 2 de tus productos en Toluca", textoAviso(bajadas(setOf(jit.id, papa.id), todos, "2026-W46"), "Toluca").first)
    }

    @Test fun presupuestoDelMandado() {
        val aguacate = prod("Aguacate Hass", 40.0, 40.0, "Frutas")
        val guajillo = prod("Chile Guajillo", 100.0, 100.0, "Chiles secos")
        val lista = listOf(
            ArticuloMandado(jitomate.id, "Jitomate saladette", 2.0),
            ArticuloMandado(aguacate.id, "Aguacate Hass", 0.5),
            ArticuloMandado(guajillo.id, "Chile guajillo", 0.25),
            ArticuloMandado("no-existe", "Mamey", 1.0),
        )
        val p = presupuesto(lista, listOf(jitomate, aguacate, guajillo))
        // 2 × 33 + 0.5 × 40 + 0.25 × 100 = 111 de mayoreo
        assertEquals(111.0, p.totalMayoreo, 0.001)
        // (66 + 20) × 1.5 + 25 × 1.4 = 164
        assertEquals(164.0, p.totalEstimado, 0.001)
        assertEquals(1, p.sinPrecio)
        assertEquals("250 g", cantidad(0.25))
        assertEquals("1 kg", cantidad(1.0))
        assertEquals("1.5 kg", cantidad(1.5))
        assertEquals(2.5, masKg(2.0), 0.0)
        assertEquals(1.75, menosKg(2.0), 0.0)
        assertTrue(mandadoTexto(p, "Toluca").contains("Total aproximado: $164"))
    }

    @Test fun resumenPorMercado() {
        val r = jitomate.resumen()!!
        assertEquals(33.0, r.actual.frec, 0.001)
        assertEquals(10.0, r.cambioSemanal!!, 0.001)
        assertEquals(31.5, r.promedio8, 0.001)
        assertEquals(null, jitomate.copy(semanas = jitomate.semanas.take(1)).resumen()!!.cambioSemanal)
        assertEquals(null, jitomate.copy(semanas = emptyList()).resumen())
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

    @Test fun csvDeMisPrecios() {
        val r = mx.acomo.app.data.RegistroPrecio(
            id = 1, fecha = "2026-11-16", productoId = "cebolla-bola-primera", producto = "Cebolla Bola",
            precio = 40.0, mayoreo = 15.0, mercado = "150", lugar = "Tianguis \"Santa Cruz\"",
            semaforo = Semaforo.ROJO,
        )
        assertEquals(40.0 / 15.0, r.factor, 0.0001)
        val csv = mx.acomo.app.data.registrosCsv(listOf(r), mapOf("150" to "Central de Abasto de Toluca")).lines()
        assertEquals("fecha,producto,precio_kg,lugar,mayoreo_kg,mercado_referencia,veces_mayoreo", csv[0])
        assertEquals(
            "2026-11-16,\"Cebolla Bola\",40.00,\"Tianguis 'Santa Cruz'\",15.00,\"Central de Abasto de Toluca\",2.67",
            csv[1],
        )
    }

    @Test fun mercadosPorDistanciaYBusqueda() {
        val toluca = Mercado("150", "Central de Abasto", "Toluca", "Estado de México", 19.27, -99.58)
        val ecatepec = Mercado("151", "Central de Abasto", "Ecatepec", "Estado de México", 19.60, -99.05)
        val merida = Mercado("310", "Central de Abasto", "Mérida", "Yucatán", 20.97, -89.62)
        val km = toluca.distanciaA(ecatepec)
        assertTrue("Toluca-Ecatepec ~66 km, fue $km", km in 55.0..75.0)
        val indice = Indice(1, "", mercados = listOf(merida, ecatepec, toluca))
        assertEquals(listOf("150", "151"), indice.cercanos(toluca, 2).map { it.id })
        assertTrue(merida.coincide("merida"))
        assertTrue(toluca.coincide("estado de mexico"))
        assertFalse(toluca.coincide("yucatan"))
    }

    /** Los JSON reales que genera el recolector deben poder leerse. */
    @Test fun leeJsonDelRecolector() {
        val json = Json { ignoreUnknownKeys = true }
        val archivoIndice = File("../../data/indice.json")
        val archivoToluca = File("../../data/mercados/150.json")
        if (!archivoIndice.exists() || !archivoToluca.exists()) return
        val indice = json.decodeFromString<Indice>(archivoIndice.readText())
        assertTrue(indice.mercados.any { it.id == "150" })
        val toluca = json.decodeFromString<DatosMercado>(archivoToluca.readText())
        assertNotNull(toluca.productos.find { it.id == "tomate-saladette-primera" }?.resumen())
    }
}
