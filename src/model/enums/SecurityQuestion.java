package model.enums;

public enum SecurityQuestion {
    Q1("What is your mother's maiden name?"),
    Q2("What was the name of your first pet?"),
    Q3("What city were you born in?"),
    Q4("What is your favorite book?"),
    Q5("What was your childhood nickname?");

    private final String text;

    SecurityQuestion(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static SecurityQuestion fromNumber(int number) {
        if (number < 1 || number > values().length) return null;
        return values()[number - 1];
    }
}
