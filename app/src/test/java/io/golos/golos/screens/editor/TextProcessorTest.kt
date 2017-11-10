package io.golos.golos.screens.editor


import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Created by yuri yurivladdurain@gmail.com on 26/10/2017.
 */
class TextProcessorTest {
    lateinit var mTextProcessor: TextProcessor

    @Before
    fun before() {
        mTextProcessor = TextProcessor()
    }

    @Test
    fun processInput() {
        var out = mTextProcessor.getInitialState()
        assertEquals("initial 1 edittext", 1, out.size)
        assert(out[0] is EditorTextPart)

        out = mTextProcessor.processInput(out, EditorInputAction.DeleteAction(null))
        assertEquals("initial 1 edittext", 1, out.size)
        assert(out[0] is EditorTextPart)

        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorImagePart("sdgdgs", "asdgsd", Part.CURSOR_POINTER_BEGIN)))
        assertEquals("initial 3 parts", 3, out.size)
        assertEquals(EditorTextPart("", null), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
        assertEquals(EditorTextPart("", Part.CURSOR_POINTER_BEGIN), out[2])

        out = mTextProcessor.processInput(ArrayList(), EditorInputAction.InsertAction(EditorTextPart("abc", "abc".length)))
        assertEquals("initial 1 edittext", 1, out.size)
        assertEquals("must appen to the end", EditorTextPart("abc", "abc".length), out[0])

        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorTextPart("lll", null)))
        assertEquals("initial 1 edittext", 1, out.size)
        assertEquals("must appen to the end", EditorTextPart("abclll", 6), out[0])

        var withOne = ArrayList(out)
        withOne[0] = EditorTextPart("abclll", 3)

        out = mTextProcessor.processInput(withOne, EditorInputAction.InsertAction(EditorTextPart("ggg", null)))
        assertEquals("initial 1 edittext", 1, out.size)
        assertEquals("must appen to the middle", EditorTextPart("abcggglll", 6), out[0])

        withOne.clear()
        withOne.add(EditorTextPart("abcggglll", 3))
        out = mTextProcessor.processInput(withOne, EditorInputAction.InsertAction(EditorImagePart("sdgdgs", "asdgsd", null)))
        assertEquals("initial 3 parts", 3, out.size)
        assertEquals(EditorTextPart("abc", null), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
        assertEquals(EditorTextPart("ggglll", Part.CURSOR_POINTER_BEGIN), out[2])

        var withthree = ArrayList(out)
        out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd1", null)))
        assertEquals("initial 5 parts", 5, out.size)
        assertEquals(EditorTextPart("abc", null), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
        assertEquals(EditorTextPart("", null), out[2])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd1", null), out[3])
        assertEquals(EditorTextPart("ggglll", Part.CURSOR_POINTER_BEGIN), out[4])

        withthree = ArrayList(out)
        withthree[withthree.lastIndex] = EditorTextPart("ggglll", "ggglll".length)

        out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd2", null)))
        assertEquals("initial 7 parts", 7, out.size)
        assertEquals(EditorTextPart("abc", null), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
        assertEquals(EditorTextPart("", null), out[2])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd1", null), out[3])
        assertEquals(EditorTextPart("ggglll", null), out[4])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd2", null), out[5])
        assertEquals(EditorTextPart("", Part.CURSOR_POINTER_BEGIN), out[6])

        withthree = ArrayList(out)
        withthree[6] = EditorTextPart("", null)
        withthree[2] = EditorTextPart("", Part.CURSOR_POINTER_BEGIN)


        out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd3", null)))
        assertEquals("initial 9 parts", 9, out.size)
        assertEquals(EditorTextPart("abc", null), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
        assertEquals(EditorTextPart("", null), out[2])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd3", null), out[3])
        assertEquals(EditorTextPart("", Part.CURSOR_POINTER_BEGIN), out[4])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd1", null), out[5])
        assertEquals(EditorTextPart("ggglll", null), out[6])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd2", null), out[7])
        assertEquals(EditorTextPart("", null), out[8])

        withthree = ArrayList(out)
        withthree[4] = EditorTextPart("", null)
        withthree[0] = EditorTextPart("abc", 2)

        out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd4", null)))
        assertEquals("initial 11 parts", 11, out.size)
        assertEquals(EditorTextPart("ab", null), out[0])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd4", null), out[1])
        assertEquals(EditorTextPart("c", 0), out[2])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[3])
        assertEquals(EditorTextPart("", null), out[10])


        //delete test
        withthree = ArrayList(out)
        out = mTextProcessor.processInput(withthree, EditorInputAction.DeleteAction(null))
        assertEquals("initial 11 parts", 11, out.size)
        assertEquals(EditorTextPart("ab", null), out[0])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd4", null), out[1])
        assertEquals(EditorTextPart("c", 0), out[2])

        withthree = ArrayList(out)
        withthree[2] = EditorTextPart("c", 1)
        out = mTextProcessor.processInput(withthree, EditorInputAction.DeleteAction(null))
        assertEquals("muste be 11 parts", 11, out.size)
        assertEquals(EditorTextPart("ab", null), out[0])
        assertEquals(EditorImagePart("sdgdgs1", "asdgsd4", null), out[1])
        assertEquals(EditorTextPart("", 0), out[2])

        withthree = ArrayList(out)
        out = mTextProcessor.processInput(withthree, EditorInputAction.DeleteAction(1))
        assertEquals("muste be 9 parts", 9, out.size)
        assertEquals(EditorTextPart("ab", 2), out[0])
        assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])

    }

    fun prepareForTestWithMultipleParts(): ArrayList<Part> {
        var out = ArrayList<Part>()
        out.add(EditorTextPart("abcggglll", null))
        out.add(EditorImagePart("sdgdgs", "asdgsd", null))
        out.add(EditorTextPart("", null))
        return out
    }
}