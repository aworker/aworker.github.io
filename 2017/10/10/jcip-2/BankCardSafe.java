/**
 * Created by lmy on 2017/10/13.
 */
public class BankCardSafe {
    private int money;


    public BankCardSafe(String userName,String userPassword){

        /**
         *  用 userName，userPassword查找数据库，初始化实例类BankCard
         */
    }

    /**
     * 查看银行卡余额
     * @return
     */
    public synchronized int getMoney(){
        return money;
    }

    /**
     * 向银行卡中加钱
     * @param addNum 加钱数
     */
    public synchronized  void addMoney(int addNum){
        money = money + addNum;
    }

    /**
     * 从银行卡中减钱
     * @param subNum  减钱数
     */
    public synchronized void subMoney(int subNum){
        if (money - subNum < 0) {
            return;
        }
    }
}
