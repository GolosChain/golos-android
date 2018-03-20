package io.golos.golos.screens.editor

import io.golos.golos.R
import io.golos.golos.utils.StringSupplier
import org.junit.Assert.*
import org.junit.Test

class TagsStringValidatorTest {
    val to_much_tags = "to_much_tags"
    val use_only_one_dash = "use_only_one_dash"
    val use_whitespaces_seporator = "use_whitespaces_seporator"
    val only_lowercase_characters = "only_lowercase_characters"
    val use_only_allowed_characters = "use_only_allowed_characters"
    val first_char_must_be_a_letter = "first_char_must_be_a_letter"

    @Test
    fun validate() {
        val validator = TagsStringValidator(object : StringSupplier {
            override fun get(id: Int, args: String?): String {
                return when (id) {
                    R.string.use_only_one_dash -> use_only_one_dash
                    R.string.to_much_tags -> to_much_tags
                    R.string.use_whitespaces_seporator -> use_whitespaces_seporator
                    R.string.only_lowercase_characters -> only_lowercase_characters
                    R.string.use_only_allowed_characters -> use_only_allowed_characters
                    R.string.first_char_must_be_a_letter -> first_char_must_be_a_letter
                    else -> throw IllegalStateException("not valid output")
                }
            }
        })
        var out = validator.validate("as")
        assertTrue(" as is valid", out.first)
        out = validator.validate("as 12")
        assertFalse(" as 12 in not valid", out.first)
        assertEquals(first_char_must_be_a_letter, out.second)

        out = validator.validate("as as3")
        assertTrue(" sr is valid", out.first)

        out = validator.validate("as as3,")
        assertFalse(" as as3,", out.first)
        assertEquals(use_whitespaces_seporator, out.second)

        out = validator.validate("as as--3")
        assertFalse(" as as--3", out.first)
        assertEquals(use_only_one_dash, out.second)

        out = validator.validate("as as3 sdsg gsdgsd sdgsdg sdg")
        assertFalse(" as as3 sdsg gsdgsd sdgsdg sdg3", out.first)
        assertEquals(to_much_tags, out.second)

        out = validator.validate("as as3 sdsg gsdgsd sdgF")
        assertFalse("as as3 sdsg gsdgsd sdgF", out.first)
        assertEquals(only_lowercase_characters, out.second)

        out = validator.validate("as as3 sdsg gsdgsd sdg`")
        assertFalse("as as3 sdsg gsdgsd sdg`F", out.first)
        assertEquals(use_only_allowed_characters, out.second)

        out = validator.validate("as as3 sdsg gsdgsd sdg")
        assertTrue("as as3 sdsg gsdgsd sdg", out.first)

        out = validator.validate("as      as3 sdsg      gsdgsd sdg")
        assertTrue("as as3 sdsg gsdgsd sdg", out.first)
    }

}