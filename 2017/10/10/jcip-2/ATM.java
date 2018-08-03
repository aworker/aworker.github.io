/**
 * Created by lmy on 2017/10/12.
 */
public class ATM {

    private BankCardSafe card;


    public ATM(String userName,String userPassword) {
        this.card = new BankCardSafe(userName, userPassword);
    }

    /**
     * 通过ATM向指定银行卡存钱
     * @param money
     */
    public void  saveMoney(int money){
        card.addMoney(money);
    }

    /**
     * 通过ATM从指定银行卡中取钱
     * @param money
     */
    public void drawMoney(int money){
        card.subMoney(money);
    }
}
