package com.bendey.restaurant.core.domain.print

import java.io.File
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class PrintCopyTest {
    @Test fun claves_y_textos_exactos() {
        assertEquals(
            mapOf(
                "print.ok" to "Impreso.",
                "print.failed" to "No se pudo imprimir: {motivo}. Revisa la impresora y vuelve a intentar.",
                "print.not_configured" to "Esta impresora no está configurada. Ve a Impresoras para elegirla.",
                "print.server_unreachable" to
                    "No encontramos el servidor de impresión de tu red. Revisa que la PC de caja esté encendida y en el mismo Wi-Fi.",
                "print.server_down" to "El servidor de impresión no respondió. Reinicia la app de caja e intenta de nuevo.",
                "print.comanda_sent_not_printed" to "Comanda #{n} enviada, pero no se imprimió.",
            ),
            PrintCopy.TEXTS,
        )
    }

    @Test fun solo_ok_cuenta_como_impreso() {
        val impresos = PrintStatus.entries.filter { PrintOutcome(it).isPrinted }
        assertEquals(listOf(PrintStatus.OK), impresos)
    }

    @Test fun cada_estado_mapea_a_su_texto() {
        assertEquals("Impreso.", PrintOutcome.Ok.message)
        assertEquals(PrintCopy.TEXTS.getValue("print.not_configured"), PrintOutcome.NotConfigured.message)
        assertEquals(PrintCopy.TEXTS.getValue("print.server_unreachable"), PrintOutcome.ServerUnreachable.message)
        assertEquals(PrintCopy.TEXTS.getValue("print.server_down"), PrintOutcome.ServerDown.message)
        assertEquals(
            "No se pudo imprimir: la impresora está apagada. Revisa la impresora y vuelve a intentar.",
            PrintOutcome.failed("la impresora está apagada").message,
        )
        assertEquals("Comanda #12 enviada, pero no se imprimió.", comandaSentNotPrinted(12))
    }

    @Test fun failed_sin_motivo_usa_el_de_defecto() {
        val m = PrintOutcome.failed(null).message
        assertFalse(m.contains("null"))
        assertTrue(m.contains("la impresora no respondió"))
    }

    private val rawReasons = listOf(
        "Error: invoke failed: os error 10061 (No connection could be made because the target machine actively refused it)",
        "connection refused", "timed out", "os error 10054",
        "Failed to invoke printers_print_raw: {\"code\":\"E_PRINT\"}",
        "thread panicked at src-tauri/src/lib.rs:10",
        "TypeError: Cannot read properties of undefined (reading \"x\")",
        "No route to host", "The printer is offline", "out of paper", "Access denied", "printer not found",
        "ERR_NETWORK", "0x80070005",
        "Impresora Windows solo disponible en escritorio", "Selecciona una impresora Windows", "", null,
    )

    @Test fun ningun_motivo_es_tecnico() {
        val bad = Regex("tauri|cargo|invoke|os error|undefined|panic|0x|\\{|\\}|ERR_", RegexOption.IGNORE_CASE)
        for (raw in rawReasons) {
            val reason = sanitizePrintReason(raw)
            assertFalse("motivo técnico: $raw -> $reason", looksTechnical(reason))
            assertFalse("rastro técnico: $raw -> $reason", bad.containsMatchIn(reason))
            val msg = PrintOutcome.failed(raw).message
            assertTrue(msg.startsWith("No se pudo imprimir: "))
            assertTrue(msg.endsWith(". Revisa la impresora y vuelve a intentar."))
        }
    }

    @Test fun traduce_lo_conocido() {
        assertEquals("la impresora rechazó la conexión", sanitizePrintReason("connection refused"))
        assertEquals("la impresora no respondió", sanitizePrintReason("timed out"))
        assertEquals(
            "a la impresora se le acabó el papel o la tapa está abierta",
            sanitizePrintReason("out of paper"),
        )
        assertEquals("no encontramos la impresora en la red", sanitizePrintReason("No route to host"))
        assertEquals("se perdió la conexión con la impresora", sanitizePrintReason("os error 10054"))
    }

    @Test fun conserva_el_texto_claro_sin_punto_final_y_en_minuscula() {
        assertEquals(
            "impresora Windows solo disponible en escritorio",
            sanitizePrintReason("Impresora Windows solo disponible en escritorio."),
        )
    }

    @Test fun no_configurada_es_un_estado_propio() {
        listOf(
            "Impresora de comandas no configurada",
            "No hay impresora de documentos configurada",
            "Impresora de documentos no configurada",
            "Impresora de precuenta no configurada",
        ).forEach {
            assertTrue(it, isNotConfiguredText(it))
            assertEquals(PrintStatus.NOT_CONFIGURED, printOutcomeFromRemoteError("unknown", it).status)
            assertEquals(PrintStatus.NOT_CONFIGURED, printOutcomeFromPlatformError(it).status)
        }
    }

    @Test fun servidor_inalcanzable_no_se_confunde_con_sin_configurar() {
        assertEquals(PrintStatus.SERVER_UNREACHABLE, printOutcomeFromRemoteError(RemotePrintCode.CONNECTION, "No se pudo conectar al servidor. Verifique la red local.").status)
        assertEquals(PrintStatus.SERVER_DOWN, printOutcomeFromRemoteError(RemotePrintCode.TIMEOUT, "Tiempo de espera agotado").status)
        assertEquals(PrintStatus.SERVER_DOWN, printOutcomeFromRemoteError(RemotePrintCode.SERVER_STOPPED, "Servidor detenido").status)
        assertEquals(PrintStatus.NOT_CONFIGURED, printOutcomeFromRemoteError(RemotePrintCode.NOT_CONFIGURED, null).status)
    }

    @Test fun error_del_servidor_con_motivo_propio_es_failed_limpio() {
        val o = printOutcomeFromRemoteError("printer_offline", "Impresora no disponible en el servidor")
        assertEquals(PrintStatus.FAILED, o.status)
        assertFalse(o.isPrinted)
        assertFalse(o.message.contains("http"))
    }

    @Test fun errores_del_transporte_local_no_filtran_ip_ni_puerto() {
        val cases = listOf(
            "Error TCP: failed to connect to /192.168.1.50 (port 9100) from /192.168.1.8 (port 40322) after 5000ms",
            "Error TCP: Connection refused",
            "Error al imprimir BT: read failed, socket might closed or timeout, read ret: -1",
            "No se pudo conectar: java.io.IOException: bt socket closed",
            "Error TCP: 192.168.1.50:9100",
            "Tiempo de espera agotado enviando datos a la impresora",
        )
        for (c in cases) {
            val msg = printOutcomeFromPlatformError(c).message
            assertFalse(msg, Regex("""\d+\.\d+\.\d+\.\d+""").containsMatchIn(msg))
            assertFalse(msg, Regex(""":\d{2,5}\b""").containsMatchIn(msg))
            assertFalse(msg, msg.contains("java", ignoreCase = true) || msg.contains("Exception"))
            assertFalse(msg, msg.contains("Error TCP"))
        }
    }

    @Test fun una_comanda_a_medias_nunca_cuenta_como_impresa() {
        assertTrue(combinePrintOutcomes(emptyList()).isPrinted)
        assertTrue(combinePrintOutcomes(listOf(PrintOutcome.Ok, PrintOutcome.Ok)).isPrinted)
        val parcial = combinePrintOutcomes(listOf(PrintOutcome.Ok, PrintOutcome.failed("la impresora está apagada"), PrintOutcome.NotConfigured))
        assertFalse(parcial.isPrinted)
        assertEquals(PrintStatus.FAILED, parcial.status)
        assertEquals(PrintStatus.NOT_CONFIGURED, combinePrintOutcomes(listOf(PrintOutcome.NotConfigured, PrintOutcome.Ok)).status)
    }

    // ---- paridad contra el archivo de Tauri (si el repo está al lado) ----

    private fun tauriSource(): String? {
        var dir: File? = File("").absoluteFile
        repeat(8) {
            val f = File(dir, "front_tenant_restaurant_tauri/src/content/printCopy.ts")
            if (f.isFile) return f.readText(Charsets.UTF_8)
            dir = dir?.parentFile
        }
        return null
    }

    @Test fun paridad_textos_con_printCopy_ts() {
        val src = tauriSource()
        assumeTrue("repo de Tauri no encontrado: paridad omitida", src != null)
        val block = Regex("""PRINT_COPY = \{(.*?)\} as const""", RegexOption.DOT_MATCHES_ALL).find(src!!)!!.groupValues[1]
        val entry = Regex("""'(print\.[a-z_]+)':\s*'((?:[^'\\]|\\.)*)'""")
        val tauri = entry.findAll(block).associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'") }
        assertEquals(PrintCopy.TEXTS, tauri)
    }

    @Test fun paridad_patrones_con_sanitizePrintReason_de_tauri() {
        val src = tauriSource()
        assumeTrue("repo de Tauri no encontrado: paridad omitida", src != null)
        val from = src!!.indexOf("export function sanitizePrintReason")
        val to = src.indexOf("export function printResultFromError")
        val region = src.substring(from, to)
        // Literales /.../ seguidos de .test( : las reglas de traducción y los 3 rastros técnicos.
        val lit = Regex("""/((?:\\.|\[(?:\\.|[^\]\\])*\]|[^/\\\n\[])+)/[a-z]*\.test\(""")
        val patterns = lit.findAll(region).map { it.groupValues[1] }.toList()
        val expected = PRINT_REASON_RULES.map { it.first } +
            listOf(TECHNICAL_PATTERN_1, TECHNICAL_PATTERN_2, TECHNICAL_PATTERN_3)
        assertEquals(expected, patterns)
        // Y los motivos traducidos, en el mismo orden.
        val reasons = Regex("""return '([^']+)'|return PRINT_REASON_FALLBACK""").findAll(region)
            .map { if (it.groupValues[1].isEmpty()) PRINT_REASON_FALLBACK else it.groupValues[1] }
            .toList()
        assertEquals(listOf(PRINT_REASON_FALLBACK) + PRINT_REASON_RULES.map { it.second } + PRINT_REASON_FALLBACK, reasons)
    }
}
