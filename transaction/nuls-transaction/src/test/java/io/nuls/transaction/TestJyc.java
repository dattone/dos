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

package io.nuls.transaction;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Page;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.info.HostInfo;
import io.nuls.rpc.info.NoUse;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.config.ConfigBean;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.model.dto.CrossTxTransferDTO;
import io.nuls.transaction.rpc.call.LedgerCall;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.transaction.utils.LoggerUtil.Log;
import static org.junit.Assert.*;

/**
 * @author: Charlie
 * @date: 2019-01-15
 */
public class TestJyc {
    static String address23 = "5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw";

    private static Chain chain;
    static int chainId = 12345;
    static int assetChainId = 12345;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";

    @BeforeClass
    public static void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8887/ws");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId));
    }

    @Test
    public void allInOne() throws NulsException {
        Log.debug("0.##########导入创世块中的一个地址##########");
        importPriKey("00c805d2d6d5e06f57fdfb1aff56ef3c2dd15eee88f36fa7d45d368c352ec5ec0d", password);//23 5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw
        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress(address23), chainId, assetId);
        Log.debug(address23 + "-----balance:{}", balance);
        List<String> accountList;
        try {
            while (true) {
                Log.debug("##################################################");
                String agentAddress = "";
                String packingAddress = "";
                String agentHash = "";
                String depositHash = "";
                {
                    Log.debug("1.##########新建两个地址，一个作为节点地址，一个作为打包地址##########");
                    //新建两个地址
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.VERSION_KEY_STR, version);
                    params.put("chainId", chainId);
                    params.put("count", 2);
                    params.put("password", password);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params);
                    assertTrue(response.isSuccess());
                    accountList = (List<String>) ((HashMap) ((HashMap) response.getResponseData()).get("ac_createAccount")).get("list");
                    agentAddress = accountList.get(0);
                    packingAddress = accountList.get(1);
                    Log.debug("agentAddress-{{}}", agentAddress);
                    Log.debug("packingAddress-{{}}", packingAddress);
                }
                {
                    Log.debug("2.##########从创世块地址转账给新创建的地址##########");
                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(address23);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(new BigInteger("25000700000000"));
                    inputs.add(inputCoin1);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(agentAddress);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(new BigInteger("25000100000000"));
                    outputs.add(outputCoin1);

                    CoinDTO outputCoin2 = new CoinDTO();
                    outputCoin2.setAddress(packingAddress);
                    outputCoin2.setPassword(password);
                    outputCoin2.setAssetsChainId(chainId);
                    outputCoin2.setAssetsId(assetId);
                    outputCoin2.setAmount(new BigInteger("500000000"));
                    outputs.add(outputCoin2);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    assertTrue(response.isSuccess());
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("transfer hash:{}", result.get("value"));
                    Log.debug("transfer from {} to {}", address23, agentAddress);
                    Log.debug("transfer from {} to {}", address23, packingAddress);
                }

                Thread.sleep(15000);
                {
                    Log.debug("3.##########给新创建的地址设置别名##########");
                    BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                    Log.debug(agentAddress + "-----balance:{}", agentBalance);
                    assertEquals(new BigInteger("25000100000000"), agentBalance);
                    BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                    Log.debug(packingAddress + "-----balance:{}", packingBalance);
                    assertEquals(new BigInteger("500000000"), packingBalance);
                    {
                        String alias = "test_" + System.currentTimeMillis();
                        Map<String, Object> params = new HashMap<>();
                        params.put(Constants.VERSION_KEY_STR, "1.0");
                        params.put("chainId", chainId);
                        params.put("address", agentAddress);
                        params.put("password", password);
                        params.put("alias", alias);
                        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
                        assertTrue(response.isSuccess());
                        HashMap result = (HashMap) ((HashMap) response.getResponseData()).get("ac_setAlias");
                        String txHash = (String) result.get("txHash");
                        Log.debug("agentAddress alias-txHash:{}", txHash);
                    }
                    {
                        String alias = "test_" + System.currentTimeMillis();
                        Map<String, Object> params = new HashMap<>();
                        params.put(Constants.VERSION_KEY_STR, "1.0");
                        params.put("chainId", chainId);
                        params.put("address", packingAddress);
                        params.put("password", password);
                        params.put("alias", alias);
                        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
                        assertTrue(response.isSuccess());
                        HashMap result = (HashMap) ((HashMap) response.getResponseData()).get("ac_setAlias");
                        String txHash = (String) result.get("txHash");
                        Log.debug("packingAddress alias-txHash:{}", txHash);
                    }
                    Thread.sleep(12000);
                    Log.debug("4.##########创建节点##########");
                    //创建节点
                    Map agentTxMap = this.createAgentTx(agentAddress, packingAddress);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", agentTxMap);
                    assertTrue(response.isSuccess());
                    Map map = (HashMap) (((HashMap) response.getResponseData()).get("cs_createAgent"));
                    agentHash = (String) map.get("txHash");
                    Log.debug("createAgent-txHash:{}", agentHash);
                }

                Thread.sleep(12000);
                {
                    Log.debug("5.##########进行委托##########");
                    Map<String, Object> dpParams = new HashMap<>();
                    dpParams.put("chainId", chainId);
                    dpParams.put("address", agentAddress);
                    dpParams.put("password", password);
                    dpParams.put("agentHash", agentHash);
                    dpParams.put("deposit", 200000 * 100000000L);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_depositToAgent");
                    depositHash = (String) dpResult.get("txHash");
                    Log.debug("deposit-txHash:{}", depositHash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("6.##########取消委托##########");
                    //取消委托
                    Map<String, Object> params = new HashMap<>();
                    params.put("chainId", chainId);
                    params.put("address", agentAddress);
                    params.put("password", password);
                    params.put("txHash", depositHash);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdraw", params);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_withdraw");
                    String hash = (String) dpResult.get("txHash");
                    Log.debug("withdraw-txHash:{}", hash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("7.##########再次委托##########");
                    //再次委托
                    Map<String, Object> dpParams = new HashMap<>();
                    dpParams.put("chainId", chainId);
                    dpParams.put("address", agentAddress);
                    dpParams.put("password", password);
                    dpParams.put("agentHash", agentHash);
                    dpParams.put("deposit", 200000 * 100000000L);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_depositToAgent");
                    depositHash = (String) dpResult.get("txHash");
                    Log.debug("deposit-txHash:{}", depositHash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("8.##########删除节点账户，制造黄牌##########");
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.VERSION_KEY_STR, version);
                    params.put("chainId", chainId);
                    params.put("address", packingAddress);
                    params.put("password", password);
                    Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_getPriKeyByAddress", params);
                    HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_getPriKeyByAddress");
                    String priKey = (String) result.get("priKey");
                    removeAccount(packingAddress, password);
                    Thread.sleep(60000);
                    Log.debug("9.##########导入节点账户，重新加入共识##########");
                    importPriKey(priKey, password);
                    Thread.sleep(60000);
                }

                {
                    Log.debug("10.##########删除节点##########");
                    //停止节点
                    Map<String, Object> txMap = new HashMap();
                    txMap.put("chainId", chainId);
                    txMap.put("address", agentAddress);
                    txMap.put("password", password);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", txMap);
                    assertTrue(response.isSuccess());
                    Map result = (HashMap) (((HashMap) response.getResponseData()).get("cs_stopAgent"));
                    String txHash = (String) result.get("txHash");
                    Log.debug("stopAgent-txHash:{}", txHash);
                }

                Thread.sleep(12000);
                {
                    Log.debug("11.##########查询刚才创建的地址余额并返还给创世块地址##########");
                    BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                    Log.debug(agentAddress + "-----balance:{}", agentBalance);
                    BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                    Log.debug(packingAddress + "-----balance:{}", packingBalance);
                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(agentAddress);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(agentBalance.subtract(new BigInteger("100000000")));
                    inputs.add(inputCoin1);
                    CoinDTO inputCoin2 = new CoinDTO();
                    inputCoin2.setAddress(packingAddress);
                    inputCoin2.setPassword(password);
                    inputCoin2.setAssetsChainId(chainId);
                    inputCoin2.setAssetsId(assetId);
                    inputCoin2.setAmount(packingBalance.subtract(new BigInteger("100000000")));
                    inputs.add(inputCoin2);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(address23);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(agentBalance.add(packingBalance).subtract(new BigInteger("200000000")));
                    outputs.add(outputCoin1);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("ac_transfer hash:{}", result.get("value"));
                    assertTrue(response.isSuccess());
                }
                Thread.sleep(12000);
                BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                Log.debug(agentAddress + "---balance:{}", agentBalance);
                BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                Log.debug(packingAddress + "---balance:{}", packingBalance);
                Log.debug("##################################################");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void batchTransfer() throws Exception {
        importPriKey("00c805d2d6d5e06f57fdfb1aff56ef3c2dd15eee88f36fa7d45d368c352ec5ec0d", password);//23 5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw
        int count = 5;
        List<String> accountList;
        {
            //新建100个地址
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, version);
            params.put("chainId", chainId);
            params.put("count", count);
            params.put("password", password);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params);
            assertTrue(response.isSuccess());
            accountList = (List<String>) ((HashMap) ((HashMap) response.getResponseData()).get("ac_createAccount")).get("list");
            assertEquals(count, accountList.size());
        }
        {
            //给这100个地址转账
            for (String account : accountList) {
                Map transferMap = new HashMap();
                transferMap.put("chainId", chainId);
                transferMap.put("remark", "transfer test");
                List<CoinDTO> inputs = new ArrayList<>();
                List<CoinDTO> outputs = new ArrayList<>();
                CoinDTO inputCoin1 = new CoinDTO();
                inputCoin1.setAddress(address23);
                inputCoin1.setPassword(password);
                inputCoin1.setAssetsChainId(chainId);
                inputCoin1.setAssetsId(assetId);
                inputCoin1.setAmount(new BigInteger("500000000000"));
                inputs.add(inputCoin1);

                CoinDTO outputCoin1 = new CoinDTO();
                outputCoin1.setAddress(account);
                outputCoin1.setPassword(password);
                outputCoin1.setAssetsChainId(chainId);
                outputCoin1.setAssetsId(assetId);
                outputCoin1.setAmount(new BigInteger("500000000000"));
                outputs.add(outputCoin1);
                transferMap.put("inputs", inputs);
                transferMap.put("outputs", outputs);

                Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                assertTrue(response.isSuccess());
                HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                Log.debug("transfer from {} to {}, hash:{}", address23, account, result.get("value"));
                Thread.sleep(5000);
            }
        }
        Thread.sleep(15000);
        {
            //100个地址之间互相转账
            while (true) {
                for (int i = 0; i < count; i++) {
                    String from = accountList.get(i % count);
                    String to = accountList.get((i + 1) % count);

                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(from);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(new BigInteger("100000000"));
                    inputs.add(inputCoin1);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(to);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(new BigInteger("100000000"));
                    outputs.add(outputCoin1);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    assertTrue(response.isSuccess());
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("transfer from {} to {}, hash:{}", from, to, result.get("value"));
                }
                Thread.sleep(15000);
            }
        }
    }

    private void importPriKey(String priKey, String pwd) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map createAgentTx(String agentAddr, String packingAddr) {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", agentAddr);
        params.put("chainId", chainId);
        params.put("deposit", 20000 * 100000000L);
        params.put("commissionRate", 10);
        params.put("packingAddress", packingAddr);
        params.put("password", password);
        params.put("rewardAddress", agentAddr);
        return params;
    }

    public void removeAccount(String address, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("password", password);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_removeAccount", params);
        assertTrue(response.isSuccess());
        Log.debug("{}", JSONUtils.obj2json(response.getResponseData()));
    }
}
