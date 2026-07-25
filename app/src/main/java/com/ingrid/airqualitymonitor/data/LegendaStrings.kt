package com.ingrid.airqualitymonitor.data

object LegendaStrings {

    data class VariavelInfo(
        val nome: String,
        val unidade: String,
        val descricao: String,
        val normRef: String? = null,
        val subItens: List<SubItemInfo> = emptyList()
    )

    data class SubItemInfo(
        val nome: String,
        val descricao: String
    )

    val variaveis = listOf(
        VariavelInfo(
            nome = "CO₂ (dióxido de carbono)",
            unidade = "ppm",
            descricao = "Gás produzido pelo metabolismo humano e liberado na respiração. " +
                "Em ambientes fechados, sua concentração indica a renovação do ar: " +
                "quanto maior o valor, menor a ventilação do ambiente.",
            normRef = "ABNT NBR 17037:2023 (item 4.1.1); ANVISA RDC 09/2003"
        ),
        VariavelInfo(
            nome = "Temperatura",
            unidade = "°C",
            descricao = "Temperatura do ar no ambiente. É um dos parâmetros físicos da qualidade " +
                "do ar interior controlados pelo sistema de climatização, com faixa de conforto " +
                "definida em norma.",
            normRef = "ABNT NBR 17037:2023 (item 3.7)"
        ),
        VariavelInfo(
            nome = "Umidade relativa",
            unidade = "%",
            descricao = "Razão entre a quantidade de vapor de água presente no ar e o máximo que " +
                "ele poderia conter naquela temperatura. Umidade muito alta favorece a " +
                "proliferação de microrganismos no ambiente.",
            normRef = "ABNT NBR 17037:2023 (itens 3.7 e 4.1.2)"
        ),
        VariavelInfo(
            nome = "Material Particulado (PM2.5 e PM10)",
            unidade = "µg/m³",
            descricao = "Partículas sólidas ou líquidas em suspensão no ar — como poeira, fumaça " +
                "(inclusive de queimadas) e fuligem. Divide-se conforme o tamanho:",
            normRef = "ABNT NBR 17037:2023 (itens 3.10 a 3.12); CONAMA 506/24, Art. 2º",
            subItens = listOf(
                SubItemInfo(
                    nome = "PM10",
                    descricao = "Partículas de até 10 micrômetros. Exemplos: poeira, pólen, mofo. " +
                        "Ficam retidas nas vias aéreas superiores."
                ),
                SubItemInfo(
                    nome = "PM2.5",
                    descricao = "Partículas de até 2,5 micrômetros. Exemplos: fumaça (inclusive de " +
                        "queimadas) e fuligem de combustão. São as mais preocupantes para a saúde " +
                        "porque penetram fundo no sistema respiratório."
                )
            )
        )
    )
}