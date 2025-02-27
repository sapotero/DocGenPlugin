package docs.gen.actions.experimental

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList

class SearchHistoryCellRenderer(private val maxWidth: Int) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>, data: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val label = super.getListCellRendererComponent(list, data, index, isSelected, cellHasFocus) as JLabel
        val value = data as? String ?: throw IllegalArgumentException()
        val fontMetrics = label.getFontMetrics(label.font)
        
        var truncatedText = value
        while (fontMetrics.stringWidth(truncatedText) > maxWidth && truncatedText.length > 3) {
            truncatedText = truncatedText.dropLast(1)
        }
        
        if (truncatedText.length < value.length) {
            truncatedText = "${truncatedText.dropLast(3)}..."
        }
        
        label.text = "${index.inc()}. $truncatedText"
        
        return label
    }
}
