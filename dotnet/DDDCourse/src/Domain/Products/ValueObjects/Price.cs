using Core.ValueObjects;

namespace Products.ValueObjects
{
    public class Price : ValueObject<Price>
    {
        public decimal Amount;
        public string Currency;

        public Price(decimal amount, string currency)
        {
            Amount = amount;
            Currency = currency;
        }
    }
}
