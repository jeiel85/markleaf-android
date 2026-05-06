val CODE_BLOCK_REGEX = Regex("""(?sm)^```.*?```""")
val text = "```kotlin\nval x = 1\n```"
println(CODE_BLOCK_REGEX.findAll(text).toList().size)
