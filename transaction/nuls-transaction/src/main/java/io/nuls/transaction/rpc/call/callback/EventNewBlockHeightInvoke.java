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

package io.nuls.transaction.rpc.call.callback;

import io.nuls.rpc.invoke.BaseInvoke;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.service.ConfirmedTxService;

import java.util.HashMap;

/**
 * @author: Charlie
 * @date: 2019-01-02
 */
public class EventNewBlockHeightInvoke extends BaseInvoke {

    private ConfirmedTxService confirmedTxService;

    private Chain chain;

    public EventNewBlockHeightInvoke(Chain chain){
        this.chain = chain;
        this.confirmedTxService = SpringLiteContext.getBean(ConfirmedTxService.class);
    }

    @Override
    public void callBack(Response response) {
        try {
           chain.getLoggerMap().get(TxConstant.LOG_TX).debug("EventNewBlockHeightInvoke 更新最新区块......");
            if (response.isSuccess()) {
                HashMap result = (HashMap)((HashMap) response.getResponseData()).get("latestHeight");
                long blockHeight = Long.valueOf(result.get("value").toString());
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug("latestHeight : {}", blockHeight);
                chain.setBestBlockHeight(blockHeight);
                confirmedTxService.processEffectCrossTx(chain, blockHeight);
            }
        } catch (NulsException e) {
            chain.getLoggerMap().get(TxConstant.LOG_TX).error(e);
        }
    }
}