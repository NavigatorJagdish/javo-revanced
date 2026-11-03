package app.javo.fixture;
/** First-party fixture. Never distributed as a Javo Video application. */
public final class MainActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.widget.TextView text = new android.widget.TextView(this);
        text.setText("Javo patch/sign/install integration fixture");
        setContentView(text);
    }
}
