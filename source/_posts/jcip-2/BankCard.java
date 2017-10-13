/**
 * Created by lmy on 2017/10/12.
 */
public class BankCard {
    private int money;


    public BankCard(String userName,String userPassword){

        /**
         *  用 userName，userPassword查找数据库，初始化实例类BankCard
         */
    }

    /**
     * 查看银行卡余额
     * @return
     */
    public int getMoney(){
        return money;
    }

    /**
     * 向银行卡中加钱
     * @param addNum 加钱数
     */
    public void addMoney(int addNum){
        money = money + addNum;
    }

    /**
     * 从银行卡中减钱
     * @param subNum  减钱数
     */
    public void subMoney(int subNum){
        if (money - subNum < 0) {
            return;
        }
    }
}
