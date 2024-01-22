namespace AODtoValidation.Dtos
{
    #region using

    using System;
    using System.Collections.Generic;
    using System.Runtime.Serialization;

    #endregion

    [Serializable]
    [DataContract]
    public class CashOrderRequest : RequestBase
    {
        [DataMember]
        public List<CashOrderItem> OrderDetails { get; set; }

        [DataMember]
        public string OwnerDealerCode { get; set; }

        [DataMember]
        public string OwnerTerminalCode { get; set; }

        [DataMember]
        public int ProvisionId { get; set; }
    }

    [Serializable]
    [DataContract]
    public class CashOrderItem
    {
        [DataMember]
        public string DiscountRate { get; set; }

        [DataMember]
        public int ProductId { get; set; }

        [DataMember]
        public short Quantity { get; set; }
    }
}