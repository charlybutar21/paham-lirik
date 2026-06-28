package com.pahamlirik.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {

    @NotBlank(message = "Lirik lagu tidak boleh kosong")
    @Size(max = 10000, message = "Lirik lagu terlalu panjang, maksimal 10.000 karakter")
    private String lyrics;
}
