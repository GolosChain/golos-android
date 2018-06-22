package io.golos.golos.screens.editor

import android.text.Selection
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Created by yuri yurivladdurain@gmail.com on 26/10/2017.
 */
@RunWith(RobolectricTestRunner::class)
class TextProcessorTest {
    lateinit var mTextProcessor: TextProcessor

    @Before
    fun before() {
        mTextProcessor = TextProcessor

    }

    @Test
    fun processInput() {
        var out = mTextProcessor.getInitialState()
        assertEquals("initial 1 edittext", 1, out.size)
        assert(out[0] is EditorTextPart)

        (out[0] as EditorTextPart).startPointer = 0
        (out[0] as EditorTextPart).endPointer = 0

        out = mTextProcessor.processInput(out,
                EditorInputAction.InsertAction(EditorImagePart(imageName = "sdgdgs",
                        imageUrl = "asdgsd")))
        assertEquals("size must be 3 parts", 3, out.size)
        assertEquals("first delimeter is empty", "", (out[0] as EditorTextPart).text.toString())
        assertEquals("asdgsd", (out[1] as EditorImagePart).imageUrl)
        assertEquals("first delimeter is empty", "", (out[2] as EditorTextPart).text.toString())
        assertEquals("cursor must be at start of last delimeter", EditorPart.CURSOR_POINTER_BEGIN,
                (out[2] as EditorTextPart).endPointer)


        var part = EditorTextPart(text = "abc".asSpannable(), startPointer = 3, endPointer = 3)
        out = mTextProcessor.processInput(ArrayList(), EditorInputAction.InsertAction(part))
        assertEquals("initial 1 edittext", 1, out.size)
        assertEquals("must append to the end",
                EditorTextPart(part.id, "abc".asSpannable(), 3, 3), out[0])


        part.endPointer = 2

        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorImagePart(imageName = "sdgdgs",
                imageUrl = "asdgsd")))
        assertEquals("initial 3 parts", 3, out.size)
        var temp: EditorTextPart = out[0] as EditorTextPart

        assertEquals("must be sliced 2 first letters", "ab".asSpannable(), temp.text)
        assertEquals("no focus", EditorPart.CURSOR_POINTER_NOT_SELECTED, temp.startPointer)
        assertEquals("no focus", EditorPart.CURSOR_POINTER_NOT_SELECTED, temp.endPointer)

        assertEquals("must be image in the center", "asdgsd", (out[1] as EditorImagePart).imageUrl)

        var third = out[2] as EditorTextPart
        assertEquals("we inserted image in middle, so it must be sliced by prelast char, with focus om start",
                "c".asSpannable(),
                third.text)
        assertEquals(third.endPointer, EditorPart.CURSOR_POINTER_BEGIN)

        val first = EditorTextPart(text = "sd".asSpannable())

        val parts = arrayListOf<EditorPart>()
        parts.apply {
            add(first)
            add(EditorImagePart(imageName = "im",
                    imageUrl = "im"))
            add(EditorTextPart(text = "".asSpannable(), startPointer = EditorPart.CURSOR_POINTER_BEGIN, endPointer = EditorPart.CURSOR_POINTER_BEGIN))

        }
        out = mTextProcessor.processInput(parts, EditorInputAction.DeleteAction(1))
        assertEquals("must be 1 part", 1, out.size)
        assertEquals("sd".asSpannable(), (out[0] as EditorTextPart).text)
        assertEquals(2, out[0].endPointer)

        parts[2] = EditorTextPart(text = "e".asSpannable(), startPointer = EditorPart.CURSOR_POINTER_BEGIN, endPointer = EditorPart.CURSOR_POINTER_BEGIN)
        out = mTextProcessor.processInput(parts, EditorInputAction.DeleteAction(1))
        assertEquals("must be 1 part", 1, out.size)
        assertEquals("sde".asSpannable(), (out[0] as EditorTextPart).text)
        assertEquals(3, out[0].endPointer)


        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorTextPart(
                text = "1".asSpannable()
        )))
        assertEquals("must be 1 part", 1, out.size)
        assertEquals("sde1".asSpannable(), (out[0] as EditorTextPart).text)
        assertEquals(4, out[0].endPointer)


        (out[0] as EditorTextPart).startPointer = 3
        (out[0] as EditorTextPart).endPointer = 3
        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorTextPart(
                text = "2".asSpannable()
        )))
        assertEquals("must be 1 part", 1, out.size)
        assertEquals("we inserted 2 before e character", "sde21".asSpannable(), (out[0] as EditorTextPart).text)
        assertEquals(4, out[0].endPointer)

        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorImagePart(
                imageName = "name", imageUrl = "imageUrl")))

        assertEquals("must be 3 part", 3, out.size)
        assertFalse(out[0].isFocused())
        assertFalse(out[1].isFocused())
        assertTrue(out[2].isFocused())

        (out as MutableList<EditorPart>)[2] = EditorTextPart(text = "test".asSpannable(), startPointer = 4, endPointer = 4)

        out = mTextProcessor.processInput(out, EditorInputAction.InsertAction(EditorImagePart(
                imageName = "name", imageUrl = "imageUrl")))
        assertEquals("must be 5 part", 5, out.size)
        assertTrue((out[4] as EditorTextPart).text.toString().isEmpty())


        val spannable = "123 123".asSpannable()
        Selection.setSelection(spannable, 0, 3)
        out = mTextProcessor.processInput(listOf(EditorTextPart(text = spannable, startPointer = 0, endPointer = 3)),
                EditorInputAction.ReplaceAction("45".asSpannable()))


        assertEquals("must be 1 part", 1, out.size)
        assertEquals("result must be 45 123", "45 123", (out[0] as EditorTextPart).text.toString())
        assertEquals("must retain start selection", 0, (out[0] as EditorTextPart).startPointer)
        assertEquals("must retain end selection", 2, (out[0] as EditorTextPart).endPointer)

        /* out = mTextProcessor.processInput(out, Edit orInputAction.InsertAction(EditorTextPart("lll", null)))
         assertEquals("initial 1 edittext", 1, out.size)
         assertEquals("must appen to the end", EditorTextPart("abclll", 6), out[0])

         var withOne = ArrayList(out)
         withOne[0] = EditorTextPart("abclll", 3)

         out = mTextProcessor.processInput(withOne, EditorInputAction.InsertAction(EditorTextPart("ggg", null)))
         assertEquals("initial 1 edittext", 1, out.size)
         assertEquals("must appen to the middle", EditorTextPart("abcggglll", 6), out[0])

         withOne.clear()
         withOne.add(EditorTextPart("abcggglll", 3))


         var withthree = ArrayList(out)
         out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd1", null)))
         assertEquals("initial 5 parts", 5, out.size)
         assertEquals(EditorTextPart("abc", null), out[0])
         assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
         assertEquals(EditorTextPart("", null), out[2])
         assertEquals(EditorImagePart("sdgdgs1", "asdgsd1", null), out[3])
         assertEquals(EditorTextPart("ggglll", EditorPart.CURSOR_POINTER_BEGIN), out[4])

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
         assertEquals(EditorTextPart("", EditorPart.CURSOR_POINTER_BEGIN), out[6])

         withthree = ArrayList(out)
         withthree[6] = EditorTextPart("", null)
         withthree[2] = EditorTextPart("", EditorPart.CURSOR_POINTER_BEGIN)


         out = mTextProcessor.processInput(withthree, EditorInputAction.InsertAction(EditorImagePart("sdgdgs1", "asdgsd3", null)))
         assertEquals("initial 9 parts", 9, out.size)
         assertEquals(EditorTextPart("abc", null), out[0])
         assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])
         assertEquals(EditorTextPart("", null), out[2])
         assertEquals(EditorImagePart("sdgdgs1", "asdgsd3", null), out[3])
         assertEquals(EditorTextPart("", EditorPart.CURSOR_POINTER_BEGIN), out[4])
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
         assertEquals(EditorImagePart("sdgdgs", "asdgsd", null), out[1])*/

    }

    fun prepareForTestWithMultipleParts(): ArrayList<EditorPart> {
        return arrayListOf()
        /*   var out = ArrayList<EditorPart>()
           out.add(EditorTextPart(text = "abcggglll", pointerPosition = null))
           out.add(EditorImagePart( imageName = "sdgdgs",
                   imageUrl = "asdgsd",
                   pointerPosition = null))
           out.add(EditorTextPart(text = "", pointerPosition = null))
           return out*/
    }

    @Test
    fun testGetSpannable() {
        val test = "qq".asSpannable()
        test.forEachIndexed({ index, char ->
            println("index = $index char = $char")
        })
    }
}