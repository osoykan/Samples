namespace AODtoValidation.Validators
{
    #region using

    using Dtos;
    using FluentValidation;

    #endregion

    public class CashOrderRequestValidator : ValidatorBase<CashOrderRequest>
    {
        public CashOrderRequestValidator()
        {
            RuleSet(ValidatorRuleSet.CashOrderMerchantRule, () =>
            {
                RuleFor(x => x.OwnerDealerCode)
                    .NotNull().WithMessage("Merchant Dealercode cannot be null");

                RuleFor(x => x.OwnerTerminalCode)
                    .NotNull().WithMessage("Merchant TerminalCode cannot be null");
            });

            RuleSet(ValidatorRuleSet.CashOrderProductRule, () =>
            {
                RuleFor(x => x.OrderDetails)
                    .Must(x => x.Count > 0).WithMessage("CashOrder must be contains at least 1 item!")
                    .NotNull().WithMessage("Order has to contains at least one product!")
                    .Must(x => x.TrueForAll(p => p.Quantity > 0))
                    .WithMessage("Product quantity must be greather than 0!");
            });
        }
    }
}