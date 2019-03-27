/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.tx.multytx;

import io.nuls.contract.tx.ContractNRC20TokenSendTxTest;
import io.nuls.contract.tx.base.BaseQuery;
import io.nuls.contract.tx.contractcallcontract.ContractCallContractSendTxTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author: PierreLuo
 * @date: 2019-03-26
 */
public class ContractMultyTxTest extends BaseQuery {

    private ContractCallContractSendTxTest contractCallContractSendTxTest;
    private ContractNRC20TokenSendTxTest contractNRC20TokenSendTxTest;

    @Before
    public void beforeTest() {
        contractCallContractSendTxTest = new ContractCallContractSendTxTest();
        contractNRC20TokenSendTxTest = new ContractNRC20TokenSendTxTest();
        // TestAddress.createAccount 生成地址，得到 importPriKey 语句，放入 contractNRC20TokenSendTxTest.importPriKeyTest 中执行
    }

    /**
     * 依赖于contractNRC20TokenSendTxTest.transfer()
     */
    @Test
    public void multyCreateNRC20() throws Exception {
        // 执行后，复制contract module日志的合约地址，赋值给成员变量contractAddress_nrc20X (X -> [0,34])
        for (int i = 0; i < 35; i++) {
            contractNRC20TokenSendTxTest.setSender(address("getToAddress", i));
            contractNRC20TokenSendTxTest.createContract();
        }
    }

    /**
     * 依赖于contractNRC20TokenSendTxTest.transfer()
     */
    @Test
    public void multyCreateContractCallContract() throws Exception {
        // 执行后，复制contract module日志的合约地址，赋值给成员变量contractAddressX (X -> [0,34])
        for (int i = 0; i < 35; i++) {
            contractCallContractSendTxTest.setSender(address("getToAddress", i));
            contractCallContractSendTxTest.createContract();
        }
    }

    private String address(String methodBaseName, int i) throws Exception {
        return this.getClass().getMethod(methodBaseName + i).invoke(this).toString();
    }

    @Test
    public void multySenderCallOneContract() throws Exception {
        // 35个sender 调用一个NRC20合约，比较时间
        int times = 35;
        contractNRC20TokenSendTxTest.setContractAddress_nrc20(address("getContractAddress_nrc20", 0));
        contractNRC20TokenSendTxTest.setMethodName("approve");
        for (int i = 0; i < times; i++) {
            contractNRC20TokenSendTxTest.setSender(address("getToAddress", i));
            contractNRC20TokenSendTxTest.callContract();
        }
    }

    @Test
    public void multySenderCallMultyContracts() throws Exception {
        // 35个sender 调用35个NRC20合约，比较时间
        int times = 35;
        contractNRC20TokenSendTxTest.setMethodName("approve");
        for (int i = 0; i < times; i++) {
            contractNRC20TokenSendTxTest.setSender(address("getToAddress", i));
            contractNRC20TokenSendTxTest.setContractAddress_nrc20(address("getContractAddress_nrc20", i));
            contractNRC20TokenSendTxTest.callContract();
        }
    }

    @Test
    public void multySenderTokenTransferToMultyContracts() throws Exception {
        // 35个sender 调用35个NRC20合约，向`contractCallContract`合约转入token
        int times = 35;
        for (int i = 0; i < times; i++) {
            contractNRC20TokenSendTxTest.setSender(address("getToAddress", i));
            contractNRC20TokenSendTxTest.setContractAddress_nrc20(address("getContractAddress_nrc20", i));
            contractNRC20TokenSendTxTest.setContractAddress(address("getContractAddress", i));
            contractNRC20TokenSendTxTest.tokenTransfer();
        }
    }

    @Test
    public void multySenderCallFiveContracts() throws Exception {
        // 35个sender 每7个调用一个合约（一共5个合约），比较时间
        int times = 35;
        contractNRC20TokenSendTxTest.setMethodName("approve");
        for (int i = 0; i < times; i++) {
            contractNRC20TokenSendTxTest.setSender(address("getToAddress", i));
            if(i % 7 == 0) {
                contractNRC20TokenSendTxTest.setContractAddress_nrc20(address("getContractAddress_nrc20", i));
            }
            contractNRC20TokenSendTxTest.callContract();
        }
    }

    @Test
    public void multySenderCallSevenContracts() throws Exception {
        // 35个sender 每5个调用一个合约（一共7个合约），方法为内部调用，内部调用的合约在外层7个合约当中，确保能够出现内部调用与外层调用冲突
        int times = 35;
        String sender;
        contractNRC20TokenSendTxTest.setMethodName("approve");
        for (int i = 0; i < times; i++) {
            sender = address("getToAddress", i);
            contractNRC20TokenSendTxTest.setSender(sender);
            contractCallContractSendTxTest.setSender(sender);
            if(i % 5 == 0) {
                if((i/5 +1)%2 == 0) {
                    contractCallContractSendTxTest.setContractAddress(address("getContractAddress", i));
                } else {
                    contractNRC20TokenSendTxTest.setContractAddress_nrc20(address("getContractAddress_nrc20", i));
                }
            }
            if(i%10 < 5) {
                contractCallContractSendTxTest.callContract_contractCallContract();
            } else {
                contractNRC20TokenSendTxTest.callContract();
            }
        }
    }

}
