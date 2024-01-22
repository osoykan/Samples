namespace AODtoValidation.Dtos
{
    #region using

    using System;
    using System.Runtime.Serialization;

    #endregion

    [Serializable]
    [DataContract]
    public class CashOrderResponse : ResponseBase
    {
        [DataMember]
        public long DiscountAmount { get; set; }

        [DataMember]
        public long NetAmount { get; set; }

        [DataMember]
        public string PromotionMessage { get; set; }

        [DataMember]
        public long TotalAmount { get; set; }
    }
}