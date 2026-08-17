package figueroa.enrique.reproducers.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.widget.CheckBox
import android.widget.LinearLayout

object InfoDialogHelper {
    fun show(
        context: Context,
        title: String,
        message: String,
        checkboxText: String,
        buttonText: String,
        onDismiss: (dontShowAgain: Boolean) -> Unit
    ) {
        val checkBox = CheckBox(context).apply {
            text = checkboxText
            setPadding(48, 24, 48, 0)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(checkBox)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(container)
            .setCancelable(false)
            .setPositiveButton(buttonText) { dialog, _ ->
                onDismiss(checkBox.isChecked)
                dialog.dismiss()
            }
            .show()
    }
}
