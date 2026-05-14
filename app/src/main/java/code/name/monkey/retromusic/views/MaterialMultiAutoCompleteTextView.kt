package code.name.monkey.retromusic.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

class MaterialMultiAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : AppCompatMultiAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun getHint(): CharSequence? {
        val parent = parent
        if (parent is TextInputLayout && parent.isProvidingHint) {
            return parent.hint
        }
        return super.getHint()
    }
}
