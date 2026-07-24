package com.bendey.restaurant.core.network.serialization

import kotlinx.serialization.json.Json

val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
    // OJO: sin esto, kotlinx omite del JSON todo campo cuyo valor coincida con su
    // default. Como el backend deserializa en structs de Go con campos NO punteros,
    // un campo ausente llega como su zero value: `active = true` (default del DTO)
    // desaparecía del cuerpo y el combo se guardaba como INACTIVO. Lo mismo aplica a
    // price_includes_igv, is_restaurant, available_for_sale, etc.
    // `explicitNulls = false` sigue omitiendo los null, así que los campos opcionales
    // (valid_from, price_override…) mantienen su semántica de "no enviado".
    encodeDefaults = true
}
