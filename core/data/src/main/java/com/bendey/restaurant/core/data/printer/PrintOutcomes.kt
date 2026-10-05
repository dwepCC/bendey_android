package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.domain.print.PrintOutcome
import com.bendey.restaurant.core.domain.print.printOutcomeFromPlatformError
import com.bendey.restaurant.platform.printing.transport.PrintResult

/** Resultado de impresión unificado (R10.4) a partir del transporte local (Bluetooth/USB/red). */
fun PrintResult.toPrintOutcome(): PrintOutcome = when (this) {
    PrintResult.Success -> PrintOutcome.Ok
    is PrintResult.Error -> printOutcomeFromPlatformError(message)
}
