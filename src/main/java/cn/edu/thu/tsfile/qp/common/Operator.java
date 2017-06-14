package cn.edu.thu.tsfile.qp.common;

/**
 * This class is a superclass of all operator. 
 * @author kangrong
 *
 */
public abstract class Operator {
    int tokenIntType;
    String tokenSymbol;

    Operator(int tokenIntType) {
        this.tokenIntType = tokenIntType;
        this.tokenSymbol = SQLConstant.tokenSymbol.get(tokenIntType);
    }

    public int getTokenIntType() {
        return tokenIntType;
    }
    
    public String getTokenSymbol() {
        return tokenSymbol;
    }

    @Override
    public String toString() {
        return tokenSymbol;
    }
}