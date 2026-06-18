# Bienvenido a Markleaf

Markleaf es un cuaderno Markdown local para Android: rápido, silencioso y basado en texto plano que tú controlas.

## Un recorrido breve

- Abre **Un lienzo Markdown bonito** para ver la superficie de escritura.
- Abre **Ritual diario de escritura** para un ejemplo tipo diario.
- Abre **Brief de proyecto** para tareas, enlaces y estructura.
- Abre **Espejo de carpeta local** cuando quieras archivos fuera de la app.

> [!TIP]
> Estas son notas normales. Puedes editarlas, exportarlas, enviarlas a la papelera o borrarlas cuando ya no las necesites.

#start #guide #inicio

---markleaf-note---

# Un lienzo Markdown bonito

![Lienzo de muestra de Markleaf](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown sigue siendo legible como texto y se convierte en una vista limpia en **Vista previa**.

## Qué muestra esta nota

- **Negrita**, _cursiva_, ~~tachado~~ y `código en línea`
- Encabezados, listas, tareas, citas, separadores, bloques de código, tablas, avisos, notas al pie, enlaces e imágenes
- Estilo de sintaxis en vivo mientras escribes

> [!NOTE]
> Cambia entre Editar y Vista previa desde la barra superior. La nota sigue siendo Markdown.

| Elemento | Para qué sirve |
| --- | --- |
| `#tag` | organizar |
| `[[Brief de proyecto]]` | enlaces locales |
| `![](...)` | adjuntar imágenes |

```kotlin
fun markleaf() = "local-first markdown"
```

Una nota al pie mantiene el detalle cerca sin interrumpir el párrafo.[^1]

[^1]: Las notas al pie, avisos, tablas y bloques de código se renderizan localmente.

#markdown #showcase #escritura

---markleaf-note---

# Ritual diario de escritura

## Página de la mañana

El objetivo no es escribir más. Es hacer fácil la primera frase.

- [x] Capturar una idea
- [ ] Convertir una tarea en nota
- [ ] Enlazar el trabajo relacionado con [[Brief de proyecto]]

> Mantén la nota lo bastante pequeña como para volver a ella.

## Cierre de la tarde

¿Qué avanzó hoy?

1. Una decisión útil
2. Una pregunta abierta
3. Algo para mañana

#journal #writing #diario

---markleaf-note---

# Brief de proyecto

Esta nota muestra cómo Markleaf puede contener un proyecto pequeño sin volverse pesado.

## Resultado

Crear un cuaderno de muestra que enseña siendo útil.

## Plan

- [x] Mostrar Markdown con buen aspecto
- [x] Incluir una imagen adjunta
- [ ] Buscar `local-first`
- [ ] Abrir backlinks desde **Ritual diario de escritura**

## Notas

Relacionado: [[Ritual diario de escritura]] y [[Etiquetas, búsqueda y backlinks]]

#project/markleaf #planning #proyecto

---markleaf-note---

# Etiquetas, búsqueda y backlinks

Escribe etiquetas en el cuerpo: #project, #writing, #privacy, #local-first.

## Ideas de búsqueda

Prueba buscar:

- `local-first`
- `folder mirror`
- `Brief de proyecto`

## Backlinks

Los wikilinks usan `[[Título de nota]]`. Cuando otra nota enlaza aquí, Markleaf puede mostrar esa relación localmente, sin cuenta ni servidor.

Ver también [[Brief de proyecto]].

#organize #search #organizar

---markleaf-note---

# Espejo de carpeta local

Markleaf no necesita una nube propia. Puedes elegir una carpeta y dejar que Android o tu herramienta de sincronización se encargue de ella.

## Qué ocurre

- Markleaf escribe cada nota como archivo Markdown.
- El frontmatter conserva el `markleaf_id` estable.
- Los adjuntos permanecen junto a las notas reflejadas.
- La app sigue sin declarar permiso INTERNET.

## Por qué importa

Tus notas siguen siendo legibles en otras herramientas Markdown, y la sincronización es tu elección.

#privacy #folder-mirror #local-first #privacidad
