# Bienvenido a Markleaf

Markleaf es una app de notas Markdown local-first para Android. Se abre rapido, te deja escribir sin ruido y guarda tu texto como Markdown plano.

## Empieza por aqui

- Toca **+** para crear una nota
- Usa la barra para aplicar encabezados, listas, negrita, codigo y citas
- Toca **Vista previa** arriba para ver el render
- Manten pulsada una nota para **fijar** o **mover a la papelera**
- Escribe `#etiqueta` en el cuerpo y se reconocera automaticamente

Las siguientes tres notas iniciales explican Markdown, organizacion por etiquetas y como tus datos se quedan en el dispositivo. Edita o borra cualquiera. #inicio #guia

---markleaf-note---

# Escribir bonito con Markdown

Markdown mantiene tu texto legible como plain text mientras admite estructura y estilo. Markleaf resalta la sintaxis en vivo mientras escribes y la renderiza limpia en **Vista previa**.

## Barra de herramientas

- **H** alterna la linea entre `#`, `##`, `###` y plano
- Los botones de vinetas, numerada y casilla activan el prefijo de linea
- Negrita, cursiva, tachado y codigo en linea envuelven la seleccion
- Cita, bloque de codigo y divisor insertan elementos de bloque
- El boton de enlace inserta una plantilla `[etiqueta](destino)`

## Enter inteligente

Al pulsar Enter al final de una linea de lista, casilla o cita, el prefijo se anade en la siguiente linea automaticamente. Las listas numeradas incrementan solas. En una linea vacia con solo prefijo, Enter cierra la lista.

## Sintaxis comun

- **Negrita** y _cursiva_
- ~~Tachado~~
- `codigo en linea`
- [ ] Tarea pendiente
- [x] Tarea hecha
- > Una cita breve

```kotlin
fun hello() = "Markleaf"
```

#markdown #escritura

---markleaf-note---

# Organiza con etiquetas

Markleaf se organiza por etiquetas en lugar de carpetas. Una nota puede llevar tantas etiquetas como quieras.

## Etiquetas inline

Escribe etiquetas como #proyecto, #reunion o #lectura en el cuerpo y aparecen en la pantalla **Etiquetas**. Toca una etiqueta para filtrar las notas que la contienen.

## Fijadas y agrupadas

Las notas fijadas suben a una seccion **Fijadas** arriba. El resto se agrupa por recencia: **Hoy**, **Ayer**, **Ultimos 7 dias**, **Anteriores**.

Manten pulsada una nota para fijarla. Vuelve a mantener pulsada una fijada para soltarla. #organizar

---markleaf-note---

# Tus datos se quedan en este dispositivo

Markleaf no declara permiso INTERNET. No hay cuenta, ni analiticas, ni anuncios, ni servidor. Tus notas viven en una base Room local hasta que tu decidas llevarlas a otro lado.

## Llevate tus notas contigo

- Comparte una nota como Markdown via el panel de compartir del sistema
- Exporta todas las notas como archivos `.md` a una carpeta que elijas
- Las notas en la papelera se pueden restaurar antes de borrarlas para siempre

La papelera es la red de seguridad contra borrados accidentales: una nota enviada a la papelera puede restaurarse desde la pantalla **Papelera**. #copia #privacidad
