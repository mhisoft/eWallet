/*
 *
 *  * Copyright (c) 2014- MHISoft LLC and/or its affiliates. All rights reserved.
 *  * Licensed to MHISoft LLC under one or more contributor
 *  * license agreements. See the NOTICE file distributed with
 *  * this work for additional information regarding copyright
 *  * ownership. MHISoft LLC licenses this file to you under
 *  * the Apache License, Version 2.0 (the "License"); you may
 *  * not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 *
 */

package org.mhisoft.wallet.action;

import org.mhisoft.common.util.HashingUtils;
import org.mhisoft.wallet.service.BeanType;
import org.mhisoft.wallet.service.ServiceRegistry;
import org.mhisoft.wallet.view.DialogUtils;
import org.mhisoft.wallet.view.PasswordForm;

/**
 * Description:   action for creating the password.
 *
 * @author Tony Xue
 * @since Apr, 2016
 */
public class CreatePasswordAction implements Action {

	@Override
	public ActionResult execute(Object... params) {
		String pass = (String) params[0];
		PasswordForm passwordForm = (PasswordForm) params[1];

		if (createPassword(pass)) {
			passwordForm.exitPasswordForm();
			DialogUtils.getInstance().info("Please keep this in a safe place, it can't be recovered\n"
					+ passwordForm.getUserInputPass() + ", combination:"
					+ passwordForm.getCombinationDisplay());

			//proceed to load wallet
			LoadWalletAction loadWalletAction = ServiceRegistry.instance.getService(BeanType.prototype, LoadWalletAction.class);
			loadWalletAction.execute(pass, ServiceRegistry.instance.getWalletModel().getPassHash());
		}



		return new ActionResult(true);

	}

	//create the hash and save to file.
	protected void createHash(String pass) {
		try {
			String hash = HashingUtils.createHash(pass);
			ServiceRegistry.instance.getWalletModel().setPassHash(hash);
			ServiceRegistry.instance.getWalletSettings().setPassPlain(pass);


		} catch (HashingUtils.CannotPerformOperationException e1) {
			e1.printStackTrace();
			DialogUtils.getInstance().error("An error occurred", "Failed to hash the password:" + e1.getMessage());
		}
	}


	public boolean createPassword(String pass) {
		ServiceRegistry.instance.getWalletSettings().setPassPlain(pass);
		createHash(pass);
		return true;
	}


}