package pt.ipt.dama.muscleup.ui.theme

import androidx.compose.ui.graphics.Color

// Fundos
val Dark900 = Color(0xFF121212)
val Dark800 = Color(0xFF1E1E1E)

// Primária — Deep Orange (fitness classic)
val Orange500 = Color(0xFFFF5722)
val Orange300 = Color(0xFFFF8A65)

// Secundária — âmbar
val Amber500 = Color(0xFFFF8F00)
val Amber300 = Color(0xFFFFB74D)

// Conteúdo
val White90 = Color(0xFFE8E8E8)
val White60 = Color(0xFF9E9E9E)

// Erro
val ErrorRed = Color(0xFFCF6679) // tom "dark" do Material 3 — usado no tema escuro

// ---------------------------------------------------------------------
// Passo 10 (fix) — tons "container" e neutros que faltavam. Sem estes, o
// Material 3 preenche os espaços em branco com o azul/roxo do tema base
// (ex: FloatingActionButton usa primaryContainer por omissão), o que
// destoava da paleta laranja/âmbar — mais visível no tema claro.
// ---------------------------------------------------------------------

// Container da primária (laranja)
val OrangeContainerLight = Color(0xFFFFDBCF)
val OnOrangeContainerLight = Color(0xFF3A0B00)
val OrangeContainerDark = Color(0xFF5C1A00)
val OnOrangeContainerDark = Color(0xFFFFB59E)

// Container da secundária (âmbar)
val AmberContainerLight = Color(0xFFFFE0B2)
val OnAmberContainerLight = Color(0xFF2A1800)
val AmberContainerDark = Color(0xFF4A2C00)
val OnAmberContainerDark = Color(0xFFFFDDB3)

// Erro — tom próprio para o tema claro (mais escuro = mais contraste em fundo branco)
val ErrorRedLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Neutros — fix principal do contraste no tema claro (texto secundário
// escuro em vez de cinzento-claro sobre fundo branco)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4CF)
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)
