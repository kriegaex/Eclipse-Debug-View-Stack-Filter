/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public class DefPkgReturnType {

		public static void main(String[] args) {
			new DefPkgReturnType().test();
		}

		private void test() {
			DefPkgReturnType object = new DefPkgReturnType();
			System.out.println(object.self());
		}
		
		protected DefPkgReturnType self() {
			return this;
		}
}
