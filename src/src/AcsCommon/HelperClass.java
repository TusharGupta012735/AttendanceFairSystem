package AcsCommon;

public class HelperClass {
	public static class SecurityAttribute {
		private Boolean bIssuerCode_;
		private Boolean bPin_;
		private Boolean bAccessCondition5_;
		private Boolean bAccessCondition4_;
		private Boolean bAccessCondition3_;
		private Boolean bAccessCondition2_;
		private Boolean bAccessCondition1_;

		public SecurityAttribute(Boolean enable) {
			bIssuerCode_ = enable;
			bPin_ = enable;
			bAccessCondition5_ = enable;
			bAccessCondition4_ = enable;
			bAccessCondition3_ = enable;
			bAccessCondition2_ = enable;
			bAccessCondition1_ = enable;
		}

		public boolean getIssuerCode() {
			return this.bIssuerCode_;
		}

		public void setIssuerCode(boolean issuerCode) {
			this.bIssuerCode_ = issuerCode;
		}

		public boolean getPin() {
			return this.bPin_;
		}

		public void setPin(boolean pin) {
			this.bPin_ = pin;
		}

		public boolean getAccessCondition5() {
			return this.bAccessCondition5_;
		}

		public void setAccessCondition5(boolean accessCondition5) {
			this.bAccessCondition5_ = accessCondition5;
		}

		public boolean getAccessCondition4() {
			return this.bAccessCondition4_;
		}

		public void setAccessCondition4(boolean accessCondition4) {
			this.bAccessCondition4_ = accessCondition4;
		}

		public boolean getAccessCondition3() {
			return this.bAccessCondition3_;
		}

		public void setAccessCondition3(boolean accessCondition3) {
			this.bAccessCondition3_ = accessCondition3;
		}

		public boolean getAccessCondition2() {
			return this.bAccessCondition2_;
		}

		public void setAccessCondition2(boolean accessCondition2) {
			this.bAccessCondition2_ = accessCondition2;
		}

		public boolean getAccessCondition1() {
			return this.bAccessCondition1_;
		}

		public void setAccessCondition1(boolean accessCondition1) {
			this.bAccessCondition1_ = accessCondition1;
		}

		public byte getRawValue() {
			byte rawValue = 0x00;

			if (bAccessCondition1_)
				rawValue |= 0x02;

			if (bAccessCondition2_)
				rawValue |= 0x04;

			if (bAccessCondition3_)
				rawValue |= 0x08;

			if (bAccessCondition4_)
				rawValue |= 0x10;

			if (bAccessCondition5_)
				rawValue |= 0x20;

			if (bPin_)
				rawValue |= 0x40;

			if (bIssuerCode_)
				rawValue |= 0x80;

			return rawValue;
		}
	}

	public static class OptionRegister {
		private Boolean bEnableAccount_;
		private Boolean bEnableTripleDes_;
		private Boolean bEnableChangePinCommand_;
		private Boolean bEnableDebitMac_;
		private Boolean bRequirePinDuringDebit_;
		private Boolean bEnableRevokeDebitCommand_;
		private Boolean bRequireMutualAuthenticationOnAccountTransaction_;
		private Boolean bRequireMutualAuthenticationOnInquireAccount_;

		public OptionRegister(Boolean enable) {
			bEnableAccount_ = enable;
			bEnableTripleDes_ = enable;
			bEnableChangePinCommand_ = enable;
			bEnableDebitMac_ = enable;
			bRequirePinDuringDebit_ = enable;
			bEnableRevokeDebitCommand_ = enable;
			bRequireMutualAuthenticationOnAccountTransaction_ = enable;
			bRequireMutualAuthenticationOnInquireAccount_ = enable;
		}

		public boolean getEnableAccount() {
			return this.bEnableAccount_;
		}

		public void setEnableAccount(boolean enableAccount) {
			this.bEnableAccount_ = enableAccount;
		}

		public boolean getEnableTripleDes() {
			return this.bEnableTripleDes_;
		}

		public void setEnableTripleDes(boolean enableTripleDes) {
			this.bEnableTripleDes_ = enableTripleDes;
		}

		public boolean getEnableChangePinCommand() {
			return this.bEnableChangePinCommand_;
		}

		public void setEnableChangePinCommand(boolean enableChangePinCommand) {
			this.bEnableChangePinCommand_ = enableChangePinCommand;
		}

		public boolean getEnableDebitMac() {
			return this.bEnableDebitMac_;
		}

		public void setEnableDebitMac(boolean enableDebitMac) {
			this.bEnableDebitMac_ = enableDebitMac;
		}

		public boolean getRequirePinDuringDebit() {
			return this.bRequirePinDuringDebit_;
		}

		public void setRequirePinDuringDebit(boolean requirePinDuringDebit) {
			this.bRequirePinDuringDebit_ = requirePinDuringDebit;
		}

		public boolean getEnableRevokeDebitCommand() {
			return this.bEnableRevokeDebitCommand_;
		}

		public void setEnableRevokeDebitCommand(boolean enableRevokeDebitCommand) {
			this.bEnableRevokeDebitCommand_ = enableRevokeDebitCommand;
		}

		public boolean getRequireMutualAuthenticationOnAccountTransaction() {
			return this.bRequireMutualAuthenticationOnAccountTransaction_;
		}

		public void setRequireMutualAuthenticationOnAccountTransaction(
				boolean requireMutualAuthenticationOnAccountTransaction) {
			this.bRequireMutualAuthenticationOnAccountTransaction_ = requireMutualAuthenticationOnAccountTransaction;
		}

		public boolean getRequireMutualAuthenticationOnInquireAccount() {
			return this.bRequireMutualAuthenticationOnInquireAccount_;
		}

		public void setRequireMutualAuthenticationOnInquireAccount(
				boolean requireMutualAuthenticationOnInquireAccount) {
			this.bRequireMutualAuthenticationOnInquireAccount_ = requireMutualAuthenticationOnInquireAccount;
		}

		public byte getRawValue() {
			byte rawValue = 0x00;

			if (bEnableAccount_)
				rawValue |= 0x01;

			if (bEnableTripleDes_)
				rawValue |= 0x02;

			if (bEnableChangePinCommand_)
				rawValue |= 0x04;

			if (bEnableDebitMac_)
				rawValue |= 0x08;

			if (bRequirePinDuringDebit_)
				rawValue |= 0x10;

			if (bEnableRevokeDebitCommand_)
				rawValue |= 0x20;

			if (bRequireMutualAuthenticationOnAccountTransaction_)
				rawValue |= 0x40;

			if (bRequireMutualAuthenticationOnInquireAccount_)
				rawValue |= 0x80;

			return rawValue;
		}
	}

	public static class SecurityOptionRegister {
		private Boolean bIssuerCode_;
		private Boolean bPin_;
		private Boolean bAccessCondition5_;
		private Boolean bAccessCondition4_;
		private Boolean bAccessCondition3_;
		private Boolean bAccessCondition2_;
		private Boolean bAccessCondition1_;

		public SecurityOptionRegister(Boolean enable) {
			bIssuerCode_ = enable;
			bPin_ = enable;
			bAccessCondition5_ = enable;
			bAccessCondition4_ = enable;
			bAccessCondition3_ = enable;
			bAccessCondition2_ = enable;
			bAccessCondition1_ = enable;
		}

		public boolean getIssuerCode() {
			return this.bIssuerCode_;
		}

		public void setIssuerCode(boolean issuerCode) {
			this.bIssuerCode_ = issuerCode;
		}

		public boolean getPin() {
			return this.bPin_;
		}

		public void setPin(boolean pin) {
			this.bPin_ = pin;
		}

		public boolean getAccessCondition5() {
			return this.bAccessCondition5_;
		}

		public void setAccessCondition5(boolean accessCondition5) {
			this.bAccessCondition5_ = accessCondition5;
		}

		public boolean getAccessCondition4() {
			return this.bAccessCondition4_;
		}

		public void setAccessCondition4(boolean accessCondition4) {
			this.bAccessCondition4_ = accessCondition4;
		}

		public boolean getAccessCondition3() {
			return this.bAccessCondition3_;
		}

		public void setAccessCondition3(boolean accessCondition3) {
			this.bAccessCondition3_ = accessCondition3;
		}

		public boolean getAccessCondition2() {
			return this.bAccessCondition2_;
		}

		public void setAccessCondition2(boolean accessCondition2) {
			this.bAccessCondition2_ = accessCondition2;
		}

		public boolean getAccessCondition1() {
			return this.bAccessCondition1_;
		}

		public void setAccessCondition1(boolean accessCondition1) {
			this.bAccessCondition1_ = accessCondition1;
		}

		public byte getRawValue() {
			byte rawValue = 0x00;

			if (bAccessCondition1_)
				rawValue |= 0x02;

			if (bAccessCondition2_)
				rawValue |= 0x04;

			if (bAccessCondition3_)
				rawValue |= 0x08;

			if (bAccessCondition4_)
				rawValue |= 0x10;

			if (bAccessCondition5_)
				rawValue |= 0x20;

			if (bPin_)
				rawValue |= 0x40;

			if (bIssuerCode_)
				rawValue |= 0x80;

			return rawValue;
		}
	}

}
