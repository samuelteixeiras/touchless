package shortcut.gdd.android.com.shortcut;

/**
 * Created by Samuel PC on 12/04/2015.
 */
public class Options {
    public String listItemText;
    public Boolean checkBoxValue;
    public String checkBoxText;
    public String btnTag;

    public Options(String listItemText, Boolean defaultValue,String tag,String checkBoxText) {
        this.listItemText = listItemText;
        this.checkBoxText = checkBoxText;
        this.checkBoxValue = defaultValue;
        this.btnTag = tag;
    }
}
