namespace AODtoValidation.Service
{
    #region using

    using System.ServiceModel;
    using Dtos;
    using Validators;

    #endregion

    [ServiceContract]
    public interface IMobileService
    {
        [OperationContract]
        [ValidateWithRule(ValidatorRuleSet.CashOrderMerchantRule, ValidatorRuleSet.CashOrderProductRule)]
        CashOrderResponse CashOrder(CashOrderRequest request);
    }
}