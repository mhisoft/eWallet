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

package org.mhisoft.wallet.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mhisoft.common.util.Encryptor;

/**
 * Description: The model for the wallet view.
 *
 * @author Tony Xue
 * @since Mar, 2016
 */
public class WalletModel {

	List<WalletItem> itemsFlatList = new ArrayList<>();
	WalletItem currentItem;
	String passHash;
	boolean modified =false;
	Encryptor encryptor;


	public WalletModel() {

	}

	public void initEncryptor(final String pass)   {
		encryptor = new Encryptor(pass);
	}


	public Encryptor getEncryptor() {
		return encryptor;
	}

	public void setEncryptor(Encryptor encryptor) {
		this.encryptor = encryptor;
	}

	public WalletItem getCurrentItem() {
		return currentItem;
	}

	public void setCurrentItem(WalletItem currentItem) {
		this.currentItem = currentItem;
	}

	public List<WalletItem> getItemsFlatList() {
		return itemsFlatList;
	}

	public void setItemsFlatList(List<WalletItem> itemsFlatList) {
		this.itemsFlatList = itemsFlatList;
		setModified(false);      //todo
	}

	public String getPassHash() {
		return passHash;
	}

	public void setPassHash(String passHash) {
		this.passHash = passHash;
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public void setupTestData() {
		//root node
		itemsFlatList.add(new WalletItem(ItemType.category, "My Default Wallet 1"));


		WalletItem item1 = new WalletItem(ItemType.item, "PNC Bank");
		item1.setURL("https://pnc.com");
		WalletItem item2 = new WalletItem(ItemType.item, "GE Bank");
		item1.setURL("https://gecapital.com");

		WalletItem item3 = new WalletItem(ItemType.item, "Audi");
		WalletItem item4 = new WalletItem(ItemType.item, "Honda");



		itemsFlatList.add(new WalletItem(ItemType.category, "Bank Info"));
		itemsFlatList.add(item1);
		itemsFlatList.add(item2);
		itemsFlatList.add(new WalletItem(ItemType.category, "Car"));
		itemsFlatList.add(item3);
		itemsFlatList.add(item4);

		buildTreeFromFlatList();
	}

	public void setupEmptyWalletData() {
		//root node
		itemsFlatList.add(new WalletItem(ItemType.category, "Default Wallet"));
		itemsFlatList.add(new WalletItem(ItemType.category, "Category 1"));
		itemsFlatList.add(new WalletItem(ItemType.item, "Item 1"));
		setModified(true);
	}

	public String dumpFlatList() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < itemsFlatList.size(); i++) {
			WalletItem item = itemsFlatList.get(i);
			sb.append(i +":").append(item.toStringJson()).append("\n");
		}
		return sb.toString();
	}

	public WalletItem getRootItem() {
		if (itemsFlatList.size()==0)
			return null;
		return itemsFlatList.get(0) ;
	}

	/**
	 * build the hierarchical relationships from the flat list.
	 * The parent and children of each item will be set.
	 */
	public void buildTreeFromFlatList() {
		if (itemsFlatList.size()==0)
			return;

		WalletItem rootNode =itemsFlatList.get(0) ;


		//reset first
		for (int i = 0; i < itemsFlatList.size(); i++) {
			WalletItem item = itemsFlatList.get(i);
			item.setChildren(null);
			item.setParent(null);
		}


		WalletItem lastParent = rootNode;
		List<WalletItem>  rootCats = new ArrayList<>();
		List<WalletItem>  tempChildList = new ArrayList<>();
		for (int i = 1; i < itemsFlatList.size(); i++) {
			WalletItem item = itemsFlatList.get(i);

			if (ItemType.category==item.getType()) {
				//rootNode.addChild(item);
				rootCats.add(item);

				if (lastParent!=item) {
					//parent changed.
					Collections.sort(tempChildList);

					for (WalletItem c : tempChildList) {
						lastParent.addChild(c);
					}
					tempChildList.clear();
					lastParent = item;
				}


			}
			else  {
				tempChildList.add(item);
				//lastParent.addChild(item);
			}
		}

		Collections.sort(rootCats);
		for (WalletItem c : rootCats) {
			rootNode.addChild(c);
		}

		Collections.sort(tempChildList);
		for (WalletItem c : tempChildList) {
			lastParent.addChild(c);
		}



	}


	/**
	 * rebuild the flat list from the tree by walking it.
	 */
	public void buildFlatListFromTree() {
		WalletItem root = itemsFlatList.get(0);
		itemsFlatList.clear();
		walkTree(root, itemsFlatList);
	}

	protected void walkTree(WalletItem parent, List<WalletItem> result) {
		result.add(parent);
		if (parent.getChildren()!=null) {
			for (WalletItem child : parent.getChildren()) {
			     walkTree(child, result);
			}
		}
	}


	public boolean isRoot(WalletItem item) {
		return  itemsFlatList.get(0).equals(item);
	}


	protected int getItemIndex(WalletItem item) {
		int index = -1;
		for (int i = 0; i < itemsFlatList.size(); i++) {
			if (itemsFlatList.get(i).equals(item)) {
				index = i;
				break;
			}
		}

		if (index==-1) {
			throw new RuntimeException("something is wrong, can't find index for the item in the flat list:" + item);
		}

		return index;
	}


	public void addItem(final WalletItem parentItem, final WalletItem newItem) {
		if ( isRoot(parentItem) ) {
			if (newItem.getType()!= ItemType.category)
			throw new RuntimeException("Can only add category items to the root.");
			parentItem.addChild(newItem);
			itemsFlatList.add(newItem);

		}
		else {

			int index;
			if (parentItem.getChildren()==null || parentItem.getChildren().size()==0)  {
				//this parent category is empty, add after it
				 index = getItemIndex(parentItem);
			}
			else {
				//find the last child of the parentItem in the flat list and insert after that
				WalletItem lastChildren = parentItem.getChildren().get(parentItem.getChildren().size() - 1);
				index = getItemIndex(lastChildren);

			}



			if (index == itemsFlatList.size() - 1)
				//last one, just append
				itemsFlatList.add(newItem);
			else
				itemsFlatList.add(index + 1, newItem);

			//add to tree structure
			parentItem.addChild(newItem);
		}
		setModified(true);
	}

	public void  removeItem(final WalletItem item) {
		item.getParent().removeChild(item);
		itemsFlatList.remove(item);
		setModified(true);
	}


	/**
	 * Find the item with the GUID on the tree.
	 * @param GUID
	 * @return
	 */
	public WalletItem getNodeByGUID(final String GUID) {
		buildFlatListFromTree();
		for (WalletItem item : itemsFlatList) {
			if (item.getSysGUID().equals(GUID))
				return item;
		}
		return null;

	}



}
