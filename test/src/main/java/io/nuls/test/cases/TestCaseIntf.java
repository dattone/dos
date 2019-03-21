package io.nuls.test.cases;

import io.nuls.api.provider.Result;
import io.nuls.test.utils.Utils;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-19 20:48
 * @Description: 功能描述
 */
public interface TestCaseIntf<T,P> {

    default String depthSpace(int depth){
        return "===".repeat(depth) + (depth > 0 ? ">" : "");
    }

    default T check(P param,int depth) throws TestFailException{
        Utils.success(depthSpace(depth)+"开始测试【"+title()+"】");
        T res = doTest(param,depth+1);
        Utils.success(depthSpace(depth) + title() + "测试通过");
        return res;
    }

    default void checkResultStatus(Result result) throws TestFailException {
        if(!result.isSuccess()){
            throw new TestFailException(result.getMessage());
        }
        if(result.getList() == null && result.getData() == null){
            throw new TestFailException(title() + "测试返回值不符合预期，返回数据为空");
        }

    }

    default void check(boolean condition,String msg) throws TestFailException {
        if(!condition){
            throw new TestFailException(title() + "测试结果不符合预期，" + msg);
        }
    }

    String title();

    T doTest(P param,int depth) throws TestFailException;

}