using System.Threading.Tasks;

using Microsoft.AspNetCore.Mvc;

using Products.Dtos;
using Products.ReadModel;

namespace Products
{
    [Route("products/read/")]
    public class ProductReadController : Controller
    {
        private readonly IProductReadModel _readModel;

        public ProductReadController(IProductReadModel readModel)
        {
            _readModel = readModel;
        }

        [HttpGet]
        [Route("by/barcode/{barcode}")]
        public Task<ProductDto> GetByBarcode(string barcode)
        {
            return _readModel.GetByBarcode(barcode);
        }
    }
}
