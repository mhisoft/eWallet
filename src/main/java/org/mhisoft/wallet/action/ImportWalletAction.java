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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import org.mhisoft.common.util.Encryptor;
import org.mhisoft.wallet.model.ItemType;
import org.mhisoft.wallet.model.WalletItem;
import org.mhisoft.wallet.model.WalletModel;
import org.mhisoft.wallet.service.BeanType;
import org.mhisoft.wallet.service.FileContent;
import org.mhisoft.wallet.service.FileContentHeader;
import org.mhisoft.wallet.service.ServiceRegistry;
import org.mhisoft.wallet.view.DialogUtils;
import org.mhisoft.wallet.view.PasswordForm;
import org.mhisoft.wallet.view.ViewHelper;

/**
 * Description: ImportWalletAction
 * need to be prototype
 *
 * @author Tony Xue
 * @since May, 2016
 */
public class ImportWalletAction implements Action {


	@Override
	public ActionResult execute(Object... params) {

		String importFile = ViewHelper.chooseFile(null);
		if (importFile != null) {

			if (new File(importFile).isFile()) {
				FileContentHeader header = ServiceRegistry.instance.getWalletService().readHeader(importFile, true);
				String importFileHash = header.getPassHash();

				//now show password form to enter the password.
				PasswordForm passwordForm = new PasswordForm(importFile);
				passwordForm.showPasswordForm(ServiceRegistry.instance.getWalletForm(), new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						String pass = passwordForm.getUserEnterPassword();
						VerifyPasswordAction verifyPasswordAction = ServiceRegistry.instance.getService(BeanType.prototype, VerifyPasswordAction.class);
						ActionResult result = verifyPasswordAction.execute(pass, importFileHash);
						if (result.isSuccess()) {
							//close the password form
							passwordForm.exitPasswordForm();

							doTheImport(importFile, pass, importFileHash);

							//reload the view.
							ServiceRegistry.instance.getWalletModel().setModified(true);
							ServiceRegistry.instance.getWalletForm().loadTree();



							DialogUtils.getInstance().info("Import successfully.");

						}
					}
				});

				//hand off to the OK listener , actionPerformed() below to do VerifyPasswordAction

				return new ActionResult(true);

			} else {
				DialogUtils.getInstance().error("Error", "Can not open file " + importFile);
			}

		}


		return new ActionResult(false);
	}


	WalletItem findItemInModel(WalletItem item) {
		for (WalletItem walletItem : ServiceRegistry.instance.getWalletModel().getItemsFlatList()) {
			if (walletItem.getSysGUID().equals(item.getSysGUID()) || walletItem.getName().equalsIgnoreCase(item.getName()))
					return walletItem;

		}
		return null;

	}




	protected void doTheImport(String filename, String importFilePass, String importFileHash) {
		Encryptor encryptor = new Encryptor(importFilePass);
		FileContent fileContent = ServiceRegistry.instance.getWalletService().readFromFile(filename, encryptor);


		WalletModel impModel = new WalletModel();
		impModel.setPassHash(importFileHash);
		impModel.setEncryptor(encryptor);
		impModel.setItemsFlatList(fileContent.getWalletItems());
		impModel.buildTreeFromFlatList();

		WalletModel model = ServiceRegistry.instance.getWalletModel();
		WalletItem root  = model.getRootItem();

	    //will change the tree structure
		try {
			for (int i = 1; i < fileContent.getWalletItems().size(); i++) {
				WalletItem impItem = fileContent.getWalletItems().get(i);
				WalletItem modelItem = findItemInModel(impItem);
				if (modelItem!=null) {

					if (!modelItem.isSame(impItem)) {

						modelItem.mergeFrom(impItem);


					}
				}
				else {
					if (impItem.getType().equals(ItemType.category)) {
						//found a new category which does not have a match
						//add it and all its children
						root.addChild(impItem);
						//jump to next cat
						i++;
						while (i<fileContent.getWalletItems().size()) {
							if (fileContent.getWalletItems().get(i).getType()==ItemType.item ) {
								i++;
								continue;
							}

						}


					}
					else {
						//it is an item, locate its parent and find a match in the current model
						WalletItem modelCat = findItemInModel(impItem.getParent());
						if (modelCat==null) {
							//not matches cat can't happen here
							throw new IllegalStateException("not matches cat can't happen here, impItem: " + impItem);
						}
						modelCat.addChild(impItem);

					}
				}
			}

			//rebuild back the list
			model.buildFlatListFromTree();

		} catch (NoSuchFieldException e) {
			DialogUtils.getInstance().error(e.getMessage());
		}


	}



}