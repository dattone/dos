package io.nuls.test.cases.account;

import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.account.AccountService;
import io.nuls.test.Config;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.tools.core.annotation.Autowired;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-20 10:35
 * @Description: 功能描述
 */
public abstract class BaseAccountCase<T,P> implements TestCaseIntf<T,P> {

    AccountService accountService = ServiceManager.get(AccountService.class);

    public static final String PASSWORD = AccountConstants.PASSWORD;


}