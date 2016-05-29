namespace AODtoValidation.Dtos
{
    #region using

    using System;
    using System.Globalization;
    using System.Runtime.Serialization;

    #endregion

    [Serializable]
    [DataContract]
    public class ResponseBase
    {
        public ResponseBase() : this("Sucess")
        {
            HostDateTime = DateTime.Now.ToString(CultureInfo.InvariantCulture);
        }

        public ResponseBase(string responseCode)
        {
            ResponseCode = responseCode;
            HostDateTime = DateTime.Now.ToString(CultureInfo.InvariantCulture);
            Status = responseCode == "Sucess" ? 1 : -1;
        }

        [DataMember]
        public string HostDateTime { get; set; }

        [DataMember]
        public string Message { get; set; }

        [DataMember]
        public string ResponseCode { get; set; }

        [DataMember]
        public int Status { get; set; }
    }
}