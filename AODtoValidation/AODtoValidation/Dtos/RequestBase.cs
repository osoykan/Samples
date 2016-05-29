namespace AODtoValidation.Dtos
{
    #region using

    using System;
    using System.Runtime.Serialization;

    #endregion

    [Serializable]
    [DataContract]
    public class RequestBase
    {
        [DataMember]
        public string ApplicationVersion { get; set; }

        [DataMember]
        public int? BatchNo { get; set; }

        [DataMember]
        public string DealerCode { get; set; }

        [DataMember]
        public string LanguageCode { get; set; }

        [DataMember]
        public double Latitude { get; set; }

        [DataMember]
        public double Longitude { get; set; }

        [DataMember]
        public int OriginatorInstitutionCode { get; set; }

        [DataMember]
        public int ParameterVersion { get; set; }

        [DataMember]
        public string Password { get; set; }

        [DataMember]
        public string ReserveInfo { get; set; }

        [DataMember]
        public string TerminalCode { get; set; }

        [DataMember]
        public string TerminalSerialNumber { get; set; }

        [DataMember]
        public int? TraceNo { get; set; }

        [DataMember]
        public Guid TrackId { get; set; }

        [DataMember]
        public string TransactionDateTime { get; set; }

        [DataMember]
        public string Username { get; set; }
    }
}