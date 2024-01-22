using System.Collections.Generic;
using System.Threading.Tasks;

using Products.Dtos;

namespace Products.ReadModel
{
    public interface IProductReadModel
    {
        Task<List<ProductDto>> GetAll();

        Task<ProductDto> GetByBarcode(string barcode);
    }
}