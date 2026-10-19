package com.example.yami.character

/**
 * Shahid Anwar pixel-art data. Hand-designed: do not edit pixels.
 * Body grid is 24 x 32 sprite pixels: head rows 0-14, torso rows 15-24, legs rows 25-31.
 * Coordinates of faces, arms and props are relative to the body grid origin (top-left of row 0).
 * '.' is transparent; every other char is a key of [PALETTE].
 */
object ShahidSprites {
    const val BODY_W = 24
    const val BODY_H = 32
    const val HEAD_ROWS = 15
    const val TORSO_TOP = 15
    const val LEGS_TOP = 25

    class Px(val x: Int, val y: Int, val c: Char)

    /** A sprite drawn with its top-left at (dx, dy) in body-grid coordinates. */
    class Sprite(val dx: Int, val dy: Int, val rows: Array<String>) {
        val w get() = rows[0].length
        val h get() = rows.size
    }

    /** Arm sprite plus the top-left of its 2x2 hand, both in body-grid coordinates. */
    class Arm(val sprite: Sprite, val handX: Int, val handY: Int)

    val PALETTE: Map<Char, Int> = mapOf(
        'K' to 0xFF1A1511.toInt(),
        'C' to 0xFF7A6B45.toInt(),
        'c' to 0xFF5F5234.toInt(),
        'h' to 0xFF958656.toInt(),
        'B' to 0xFF2B2620.toInt(),
        'G' to 0xFFE3B341.toInt(),
        'g' to 0xFFA97F22.toInt(),
        'V' to 0xFF191612.toInt(),
        'v' to 0xFF4A443B.toInt(),
        'F' to 0xFFC98B5F.toInt(),
        'f' to 0xFFA46D47.toInt(),
        'P' to 0xFF7A2E26.toInt(),
        'M' to 0xFF1C1510.toInt(),
        'W' to 0xFFF3EEE4.toInt(),
        'S' to 0xFF7D6E47.toInt(),
        's' to 0xFF5E5233.toInt(),
        'H' to 0xFF978857.toInt(),
        'R' to 0xFFB23A2E.toInt(),
        'N' to 0xFFECE6D6.toInt(),
        'n' to 0xFF3A342C.toInt(),
        'L' to 0xFF2A2119.toInt(),
        'T' to 0xFF6A5C38.toInt(),
        't' to 0xFF4F4429.toInt(),
        'O' to 0xFF16120E.toInt(),
        'o' to 0xFF4A4036.toInt(),
        'Y' to 0xFFC9CED6.toInt(),
        'y' to 0xFF8A919C.toInt(),
        'A' to 0xFF5A3A22.toInt(),
        'a' to 0xFF3A2414.toInt(),
        'D' to 0xFF7CC4F2.toInt(),
        'X' to 0xFFE8E2D4.toInt(),
        'x' to 0xFFB9B2A2.toInt(),
        'Z' to 0xFFF2C94C.toInt(),
        'E' to 0xFFD9483B.toInt(),
        'Q' to 0xFFF6F1E7.toInt(),
        'q' to 0xFFC7372B.toInt(),
        'U' to 0xFFD2725A.toInt()
    )


    val HEAD = arrayOf(
        "......KKKKKKKKKKKK......",
        ".....KChhhhCCCCCCCK.....",
        "....KcCCCCCGGCCCCCcK....",
        "....KcCCCCGggGCCCCcK....",
        "....KBBBBBBGGBBBBBBK....",
        "...KVVvvvvVVVVVVVVVVK...",
        "....KKffffffffffffKK....",
        ".....KFMMMFFFFMMMFK.....",
        "....KfFFMWFFFFMWFFfK....",
        "....KfFFMMFFFFMMFFfK....",
        ".....KFFFFFffFFFFFK.....",
        ".....KFMMMMMMMMMMFK.....",
        ".....KMMFFFFFFFFMMK.....",
        "......KFFFffffFFFK......",
        ".......KKFFFFFFKK......."
    )

    val TORSO = arrayOf(
        ".....KSSSScffcSSSSK.....",
        "....KsGsSSSccSSSsGsK....",
        "....KRRSSSSSsSSSSSSK....",
        "....KSsRssSSGSNnnNSK....",
        "....KSsSYsSSsSSSSSSK....",
        "....KSsSSsSSsSSSSSSK....",
        "....KSssssSSGSSSSSSK....",
        "....KSSSSSSSsSSSSSSK....",
        "....KLLLLLLGGLLLLLLK....",
        ".....KTTTTTttTTTTTK....."
    )

    /** Leg grids. Drawn with their top row at body row 25. "tuck" and "sit" are 4 rows. */
    val LEGS: Map<String, Array<String>> = mapOf(
        "stand" to arrayOf(
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            ".....KOoOOOKKOoOOOK.....",
            ".....KOOOOOKKOOOOOK.....",
            ".....KKKKKKKKKKKKKK....."
        ),
        "step_a" to arrayOf(
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            ".....KOoOOOKKtTTTK......",
            ".....KOOOOOKKOoOOOK.....",
            ".....KKKKKKKKOOOOOK.....",
            ".............KKKKKK....."
        ),
        "step_b" to arrayOf(
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKOOOoOK.....",
            ".....KOOOoOKKOOOOOK.....",
            ".....KOOOOOKKKKKKKK.....",
            ".....KKKKKK............."
        ),
        "tuck" to arrayOf(
            ".....KTTTtKKKKtTTTK.....",
            "....KOoOOOKK..KOoOOK....",
            "....KOOOOOK....KOOOOK...",
            "....KKKKKKK....KKKKKK..."
        ),
        "dangle_a" to arrayOf(
            ".....KTTTtK..KtTTTK.....",
            ".....KTTTtK..KtTTTK.....",
            "....KTTTtK....KtTTTK....",
            "....KOoOK......KOoOK....",
            "....KOOOK......KOOOK....",
            "....KOOOK......KOOOK....",
            ".....KKK........KKK....."
        ),
        "dangle_b" to arrayOf(
            ".....KTTTtK..KtTTTK.....",
            ".....KTTTtK..KtTTTK.....",
            "....KTTTtK....KtTTTK....",
            "....KOoOK......KOoOK....",
            "....KOOOK......KOOOK....",
            "....KOOOK......KOOOK....",
            ".....KKK........KKK....."
        ),
        "tap" to arrayOf(
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            "......KTTTtKKtTTTK......",
            ".....KOoOOOKKOoOOOK.....",
            ".....KOOOOOKK.KOOOOK....",
            ".....KKKKKKKK..KKKK....."
        ),
        "sit" to arrayOf(
            "...KTTTTtKKKKtTTTTK.....",
            "..KOoOOOK....KOoOOOK....",
            "..KOOOOOK....KOOOOOK....",
            "..KKKKKKK....KKKKKKK...."
        )
    )

    /** Face patches applied over [HEAD]; "normal" is the head as drawn. */
    val FACES: Map<String, List<Px>> = mapOf(
        "normal" to listOf(),
        "blink" to listOf(Px(8, 8, 'F'), Px(9, 8, 'F'), Px(14, 8, 'F'), Px(15, 8, 'F'), Px(8, 9, 'M'), Px(9, 9, 'M'), Px(14, 9, 'M'), Px(15, 9, 'M')),
        "look_l" to listOf(Px(8, 8, 'W'), Px(9, 8, 'F'), Px(14, 8, 'W'), Px(15, 8, 'F'), Px(8, 9, 'M'), Px(9, 9, 'F'), Px(14, 9, 'M'), Px(15, 9, 'F'), Px(7, 8, 'M'), Px(7, 9, 'M'), Px(13, 8, 'M'), Px(13, 9, 'M')),
        "look_r" to listOf(Px(8, 8, 'F'), Px(9, 8, 'M'), Px(14, 8, 'F'), Px(15, 8, 'M'), Px(8, 9, 'F'), Px(9, 9, 'M'), Px(14, 9, 'F'), Px(15, 9, 'M'), Px(10, 8, 'W'), Px(10, 9, 'M'), Px(16, 8, 'W'), Px(16, 9, 'M')),
        "happy" to listOf(Px(8, 8, 'M'), Px(9, 8, 'M'), Px(14, 8, 'M'), Px(15, 8, 'M'), Px(8, 9, 'F'), Px(9, 9, 'F'), Px(14, 9, 'F'), Px(15, 9, 'F'), Px(7, 7, 'F'), Px(8, 7, 'F'), Px(9, 7, 'F'), Px(14, 7, 'F'), Px(15, 7, 'F'), Px(16, 7, 'F'), Px(7, 9, 'M'), Px(10, 9, 'M'), Px(13, 9, 'M'), Px(16, 9, 'M')),
        "worried" to listOf(Px(8, 8, 'F'), Px(9, 8, 'F'), Px(14, 8, 'F'), Px(15, 8, 'F'), Px(8, 9, 'M'), Px(9, 9, 'M'), Px(14, 9, 'M'), Px(15, 9, 'M'), Px(7, 7, 'F'), Px(8, 7, 'M'), Px(9, 7, 'M'), Px(14, 7, 'M'), Px(15, 7, 'M'), Px(16, 7, 'F'), Px(7, 8, 'M'), Px(16, 8, 'M')),
        "angry" to listOf(Px(8, 8, 'F'), Px(9, 8, 'M'), Px(14, 8, 'M'), Px(15, 8, 'F'), Px(8, 9, 'M'), Px(9, 9, 'M'), Px(14, 9, 'M'), Px(15, 9, 'M'), Px(7, 7, 'M'), Px(8, 7, 'M'), Px(9, 7, 'F'), Px(14, 7, 'F'), Px(15, 7, 'M'), Px(16, 7, 'M'), Px(10, 8, 'M'), Px(13, 8, 'M'), Px(6, 10, 'U'), Px(17, 10, 'U')),
        "furious" to listOf(Px(8, 8, 'F'), Px(9, 8, 'M'), Px(14, 8, 'M'), Px(15, 8, 'F'), Px(8, 9, 'E'), Px(9, 9, 'E'), Px(14, 9, 'E'), Px(15, 9, 'E'), Px(7, 7, 'M'), Px(8, 7, 'M'), Px(9, 7, 'F'), Px(14, 7, 'F'), Px(15, 7, 'M'), Px(16, 7, 'M'), Px(10, 8, 'M'), Px(13, 8, 'M'), Px(6, 10, 'U'), Px(7, 10, 'U'), Px(16, 10, 'U'), Px(17, 10, 'U'), Px(9, 12, 'P'), Px(10, 12, 'P'), Px(11, 12, 'P'), Px(12, 12, 'P'), Px(13, 12, 'P'), Px(14, 12, 'P'), Px(9, 13, 'P'), Px(14, 13, 'P'), Px(10, 13, 'W'), Px(11, 13, 'W'), Px(12, 13, 'W'), Px(13, 13, 'W')),
        "surprised" to listOf(Px(8, 8, 'W'), Px(9, 8, 'W'), Px(14, 8, 'W'), Px(15, 8, 'W'), Px(8, 9, 'W'), Px(9, 9, 'M'), Px(14, 9, 'M'), Px(15, 9, 'W'), Px(7, 7, 'F'), Px(8, 7, 'F'), Px(9, 7, 'F'), Px(14, 7, 'F'), Px(15, 7, 'F'), Px(16, 7, 'F'), Px(8, 6, 'M'), Px(9, 6, 'M'), Px(14, 6, 'M'), Px(15, 6, 'M'), Px(11, 12, 'P'), Px(12, 12, 'P'), Px(11, 13, 'P'), Px(12, 13, 'P')),
        "dizzy" to listOf(Px(8, 8, 'M'), Px(9, 8, 'F'), Px(14, 8, 'F'), Px(15, 8, 'M'), Px(8, 9, 'F'), Px(9, 9, 'M'), Px(14, 9, 'M'), Px(15, 9, 'F'), Px(7, 7, 'M'), Px(8, 7, 'F'), Px(9, 7, 'M'), Px(14, 7, 'M'), Px(15, 7, 'F'), Px(16, 7, 'M'), Px(7, 9, 'M'), Px(16, 9, 'M'), Px(10, 12, 'P'), Px(11, 13, 'P'), Px(12, 12, 'P'), Px(13, 13, 'P')),
        "shout" to listOf(Px(9, 12, 'P'), Px(10, 12, 'W'), Px(11, 12, 'W'), Px(12, 12, 'W'), Px(13, 12, 'W'), Px(14, 12, 'P'), Px(10, 13, 'P'), Px(11, 13, 'P'), Px(12, 13, 'P'), Px(13, 13, 'P')),
        "whistle" to listOf(Px(5, 11, 'F'), Px(5, 12, 'F'), Px(18, 11, 'F'), Px(18, 12, 'F'), Px(4, 11, 'K'), Px(4, 12, 'K'), Px(19, 11, 'K'), Px(19, 12, 'K'), Px(6, 11, 'U'), Px(17, 11, 'U'), Px(11, 12, 'Y'), Px(12, 12, 'Y'), Px(13, 12, 'Y'), Px(14, 12, 'y'), Px(13, 13, 'y'))
    )

    /** Arm poses. RIGHT = viewer's right side of the sprite, LEFT = mirrored. */
    val ARMS_RIGHT: Map<String, Arm> = mapOf(
        "down" to Arm(Sprite(19, 14, arrayOf(
                ".KK.",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KFFK",
                "KFFK",
                ".KK."
            )), 20, 22),
        "up" to Arm(Sprite(19, 4, arrayOf(
                "..KK.",
                ".KFFK",
                ".KFFK",
                ".KSSK",
                ".KSSK",
                ".KSSK",
                ".KSSK",
                ".KSSK",
                ".KSSK",
                "KSSSK",
                "KSSK.",
                "KSSK.",
                "KSSK.",
                ".KK.."
            )), 21, 5),
        "out" to Arm(Sprite(19, 15, arrayOf(
                ".KKKKKKKKK.",
                "KSSSSSSSFFK",
                "KSSSSSSSFFK",
                ".KKKKKKKKK."
            )), 27, 16),
        "wave_a" to Arm(Sprite(19, 7, arrayOf(
                ".....KK.",
                "....KFFK",
                "....KFFK",
                "....KSSK",
                "...KSSSK",
                "...KSSK.",
                "..KSSSK.",
                ".KSSSK..",
                "KSSSK...",
                "KSSK....",
                ".KK....."
            )), 24, 8),
        "wave_b" to Arm(Sprite(19, 7, arrayOf(
                "...KK..",
                "..KFFK.",
                "..KFFK.",
                "..KSSK.",
                "..KSSSK",
                "...KSSK",
                "..KSSSK",
                ".KSSSK.",
                "KSSSK..",
                "KSSK...",
                ".KK...."
            )), 22, 8),
        "hip" to Arm(Sprite(18, 14, arrayOf(
                "..KK...",
                ".KSSK..",
                ".KSSK..",
                ".KSSSK.",
                "..KSSSK",
                "...KSSK",
                "..KSSSK",
                ".KSSSK.",
                "KFFSK..",
                "KFFK...",
                ".KK...."
            )), 19, 22),
        "salute" to Arm(Sprite(17, 4, arrayOf(
                ".KK.....",
                "KFFK....",
                "KFFSK...",
                ".KSSK...",
                ".KSSSK..",
                "..KSSSK.",
                "...KSSK.",
                "...KSSSK",
                "....KSSK",
                "...KSSSK",
                "..KSSSK.",
                "..KSSK..",
                "..KSSK..",
                "...KK..."
            )), 18, 5),
        "mouth" to Arm(Sprite(15, 11, arrayOf(
                ".KK......",
                "KFFK.....",
                "KFFSK....",
                ".KSSKKK..",
                ".KSSSSSK.",
                "..KSSSSK.",
                "...KSSSK.",
                "....KSSSK",
                "....KSSSK",
                ".....KSSK",
                "......KK."
            )), 16, 12),
        "stache" to Arm(Sprite(17, 10, arrayOf(
                ".KK.....",
                "KFFK....",
                "KFFSK...",
                ".KSSSK..",
                "..KSSK..",
                "..KSSSK.",
                "..KSSSSK",
                "...KSSSK",
                "....KKK."
            )), 18, 11),
        "across" to Arm(Sprite(12, 14, arrayOf(
                "........KK.",
                ".......KSSK",
                ".......KSSK",
                ".......KSSK",
                ".....KKKSSK",
                ".KKKKSSSSSK",
                "KFFSSSSSSSK",
                "KFFSSSKKKK.",
                ".KKKKK....."
            )), 13, 20),
        "flail_a" to Arm(Sprite(19, 6, arrayOf(
                ".......KK.",
                "......KFFK",
                "......KFFK",
                ".....KSSSK",
                "....KSSSK.",
                "....KSSK..",
                "...KSSSK..",
                "..KSSSK...",
                ".KSSSK....",
                "KSSSK.....",
                "KSSK......",
                ".KK......."
            )), 26, 7),
        "flail_b" to Arm(Sprite(19, 12, arrayOf(
                ".......KKK.",
                "...KKKKSFFK",
                ".KKSSSSSFFK",
                "KSSSSSSSKK.",
                "KSSSKKKK...",
                ".KKK......."
            )), 27, 13)
    )
    val ARMS_LEFT: Map<String, Arm> = mapOf(
        "down" to Arm(Sprite(1, 14, arrayOf(
                ".KK.",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KSSK",
                "KFFK",
                "KFFK",
                ".KK."
            )), 2, 22),
        "up" to Arm(Sprite(0, 4, arrayOf(
                ".KK..",
                "KFFK.",
                "KFFK.",
                "KSSK.",
                "KSSK.",
                "KSSK.",
                "KSSK.",
                "KSSK.",
                "KSSK.",
                "KSSSK",
                ".KSSK",
                ".KSSK",
                ".KSSK",
                "..KK."
            )), 1, 5),
        "out" to Arm(Sprite(-6, 15, arrayOf(
                ".KKKKKKKKK.",
                "KFFSSSSSSSK",
                "KFFSSSSSSSK",
                ".KKKKKKKKK."
            )), -5, 16),
        "wave_a" to Arm(Sprite(-3, 7, arrayOf(
                ".KK.....",
                "KFFK....",
                "KFFK....",
                "KSSK....",
                "KSSSK...",
                ".KSSK...",
                ".KSSSK..",
                "..KSSSK.",
                "...KSSSK",
                "....KSSK",
                ".....KK."
            )), -2, 8),
        "wave_b" to Arm(Sprite(-2, 7, arrayOf(
                "..KK...",
                ".KFFK..",
                ".KFFK..",
                ".KSSK..",
                "KSSSK..",
                "KSSK...",
                "KSSSK..",
                ".KSSSK.",
                "..KSSSK",
                "...KSSK",
                "....KK."
            )), 0, 8),
        "hip" to Arm(Sprite(-1, 14, arrayOf(
                "...KK..",
                "..KSSK.",
                "..KSSK.",
                ".KSSSK.",
                "KSSSK..",
                "KSSK...",
                "KSSSK..",
                ".KSSSK.",
                "..KSFFK",
                "...KFFK",
                "....KK."
            )), 3, 22),
        "salute" to Arm(Sprite(-1, 4, arrayOf(
                ".....KK.",
                "....KFFK",
                "...KSFFK",
                "...KSSK.",
                "..KSSSK.",
                ".KSSSK..",
                ".KSSK...",
                "KSSSK...",
                "KSSK....",
                "KSSSK...",
                ".KSSSK..",
                "..KSSK..",
                "..KSSK..",
                "...KK..."
            )), 4, 5),
        "mouth" to Arm(Sprite(0, 11, arrayOf(
                "......KK.",
                ".....KFFK",
                "....KSFFK",
                "..KKKSSK.",
                ".KSSSSSK.",
                ".KSSSSK..",
                ".KSSSK...",
                "KSSSK....",
                "KSSSK....",
                "KSSK.....",
                ".KK......"
            )), 6, 12),
        "stache" to Arm(Sprite(-1, 10, arrayOf(
                ".....KK.",
                "....KFFK",
                "...KSFFK",
                "..KSSSK.",
                "..KSSK..",
                ".KSSSK..",
                "KSSSSK..",
                "KSSSK...",
                ".KKK...."
            )), 4, 11),
        "across" to Arm(Sprite(1, 14, arrayOf(
                ".KK........",
                "KSSK.......",
                "KSSK.......",
                "KSSK.......",
                "KSSKKK.....",
                "KSSSSSKKKK.",
                "KSSSSSSSFFK",
                ".KKKKSSSFFK",
                ".....KKKKK."
            )), 9, 20),
        "flail_a" to Arm(Sprite(-5, 6, arrayOf(
                ".KK.......",
                "KFFK......",
                "KFFK......",
                "KSSSK.....",
                ".KSSSK....",
                "..KSSK....",
                "..KSSSK...",
                "...KSSSK..",
                "....KSSSK.",
                ".....KSSSK",
                "......KSSK",
                ".......KK."
            )), -4, 7),
        "flail_b" to Arm(Sprite(-6, 12, arrayOf(
                ".KKK.......",
                "KFFSKKKK...",
                "KFFSSSSSKK.",
                ".KKSSSSSSSK",
                "...KKKKSSSK",
                ".......KKK."
            )), -5, 13)
    )

    /** Small prop sprites; the overlay engine places them (dx/dy here are 0). */
    val PROPS: Map<String, Sprite> = mapOf(
        "baton" to Sprite(0, 0, arrayOf(".K.", "KaK", "KAK", "KAK", "KAK", "KAK", "KAK", "KAK", "KAK", "KAK", ".K.")),
        "whistle_notes" to Sprite(0, 0, arrayOf("..Z.", "Z..Z", ".Z..", "...Z")),
        "steam_a" to Sprite(0, 0, arrayOf("XX..", "Xx..", "....", "..XX", "..xX")),
        "steam_b" to Sprite(0, 0, arrayOf("..XX", "..xX", "XX..", "Xx..", "....")),
        "sweat" to Sprite(0, 0, arrayOf(".D.", "DDD", ".D.")),
        "star" to Sprite(0, 0, arrayOf(".Z.", "ZZZ", ".Z.")),
        "bang" to Sprite(0, 0, arrayOf("EE", "EE", "EE", "EE", "..", "EE")),
        "challan" to Sprite(0, 0, arrayOf("qqqqqq", "QQQQQQ", "QxxxxQ", "QQQQQQ", "QxxxQQ", "QQQQQQ", "QQQQQQ")),
        "dust" to Sprite(0, 0, arrayOf(".xx.", "xXXx", ".xx.")),
        "zzz" to Sprite(0, 0, arrayOf("ZZZ", "..Z", ".Z.", "ZZZ")),
        "heart" to Sprite(0, 0, arrayOf("E.E", "EEE", ".E."))
    )
}
