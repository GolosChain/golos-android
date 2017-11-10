package io.golos.golos.screens.editor

import io.golos.golos.R
import io.golos.golos.utils.StringSupplier
import io.golos.golos.utils.StringValidator

/**
 * Created by yuri yurivladdurain@gmail.com on 28/10/2017.
 * cats.length > 5 ? tt('use_limitied_amount_of_categories', {amount: 5}) :
cats.find(c => c.length > 24)           ? tt('category_selector_jsx.maximum_tag_length_is_24_characters') :
cats.find(c => c.split('-').length > 2) ? tt('category_selector_jsx.use_one_dash') :
cats.find(c => c.indexOf(',') >= 0)     ? tt('category_selector_jsx.use_spaces_to_separate_tags') :
cats.find(c => /[A-ZА-ЯЁҐЄІЇ]/.test(c))      ? tt('category_selector_jsx.use_only_lowercase_letters') :
// Check for English and Russian symbols
cats.find(c => '18+' !== c && !/^[a-zа-яё0-9-ґєії]+$/.test(c)) ? tt('category_selector_jsx.use_only_allowed_characters') :
cats.find(c => '18+' !== c && !/^[a-zа-яё-ґєії]/.test(c)) ? tt('category_selector_jsx.must_start_with_a_letter') :

cats.find(c => '18+' !== c && !/[a-zа-яё0-9ґєії]$/.test(c)) ? tt('category_selector_jsx.must_end_with_a_letter_or_number') :
 */
class TagsStringValidator(private val supplier: StringSupplier) : StringValidator {
    override fun validate(input: String): Pair<Boolean, String> {
        val input = input.trim().replace(Regex("\\s+"), " ")
        val out = ArrayList(input.split(" "))
        if (out.size > 5) {
            return Pair(false, supplier.get(R.string.to_much_tags))
        } else if (out.any { it.split("-").size > 2 }) return Pair(false, supplier.get(R.string.use_only_one_dash))
        else if (out.any { it.contains(",") }) return Pair(false, supplier.get(R.string.use_whitespaces_seporator))
        else if (out.any { it.contains(Regex("[A-ZА-ЯЁҐЄІЇ]")) }) return Pair(false, supplier.get(R.string.only_lowercase_characters))
        else if (out.any { it != "18+" && !it.contains(Regex("^[a-zа-яё0-9-ґєії]+$")) }) return Pair(false, supplier.get(R.string.use_only_allowed_characters))
        else if (out.any { it != "18+" && !it.contains(Regex("^[a-zа-яё-ґєії]")) }) return Pair(false, supplier.get(R.string.first_char_must_be_a_letter))
        else if (out.any { it != "18+" && !it.contains(Regex("[a-zа-яё0-9ґєії]$")) }) return Pair(false, supplier.get(R.string.use_only_allowed_characters))
        else return Pair(true, "")
    }
}