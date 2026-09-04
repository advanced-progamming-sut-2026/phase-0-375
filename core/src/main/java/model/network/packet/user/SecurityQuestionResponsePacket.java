package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class SecurityQuestionResponsePacket extends Packet {
    private boolean success;
    private String message;
    private String question;
    private int questionNumber;

    public SecurityQuestionResponsePacket() {
        super(PacketType.SECURITY_QUESTION_RESPONSE);
    }

    public SecurityQuestionResponsePacket(boolean success, String message, String question, int questionNumber) {
        super(PacketType.SECURITY_QUESTION_RESPONSE);
        this.success = success;
        this.message = message;
        this.question = question;
        this.questionNumber = questionNumber;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }
}
